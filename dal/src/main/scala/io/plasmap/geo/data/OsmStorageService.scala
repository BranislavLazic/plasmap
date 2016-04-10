package io.plasmap.geo.data

import java.nio.ByteBuffer


import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.utils.Bytes._
import com.datastax.driver.core.{Cluster, ResultSet, Row, Session}
import io.plasmap.geo.data.impl.cassandra.resultset._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.data.impl.DatabaseFactory
import io.plasmap.model._
import io.plasmap.serializer.OsmDenormalizedSerializer._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


object OsmStorageService {

  val log = Logger(LoggerFactory.getLogger(OsmStorageService.getClass.getName))
  private val config = ConfigFactory.load()

  import scala.collection.JavaConversions._

  private val CASS_HOSTS: String = "plasmap.db.cassandra.hosts"
  private val CASS_PORT: String = "plasmap.db.cassandra.port"
  private val CASS_USERNAME: String = "plasmap.db.cassandra.username"
  private val CASS_PASSWORD: String = "plasmap.db.cassandra.password"

  private val hostConfig = config.getStringList(CASS_HOSTS).toList
  private val portConfig: Int = config.getInt(CASS_PORT)
  private val usernameConfig: String = config.getString(CASS_USERNAME)
  private val passwordConfig: Option[String] = if (config.hasPath(CASS_PASSWORD)) {
    Some(config.getString(CASS_PASSWORD))
  } else {
    None
  }
  private val keyspaceConfig: String = config.getString("plasmap.db.cassandra.keyspace")

  log.info(s"${Console.GREEN}Loaded config [host=$hostConfig,port=$portConfig,user=$usernameConfig,password=${passwordConfig.isDefined}${Console.RESET}")

  private val cluster: Cluster = DatabaseFactory.newDatabase(
    hosts = hostConfig,
    port = portConfig,
    username = usernameConfig,
    password = passwordConfig)

  val session: Session = cluster.connect(keyspaceConfig)

  val insertNodeBBStatement = session.prepare("INSERT INTO osm_nodes_bb(bb, osmid, node) VALUES (?, ?, ?);")
  val insertWayBBStatement = session.prepare("INSERT INTO osm_ways_bb(bb, osmid, way) VALUES (?, ?, ?);")
  val insertRelationBBStatement = session.prepare("INSERT INTO osm_relations_bb(bb, osmid, relation) VALUES (?, ?, ?);")

  val insertNodeBBTagStatement = session.prepare("INSERT INTO osm_nodes_bb_tag(bb, osmid, key, value, node) VALUES (?, ?, ?, ?, ?);")
  val insertWayBBTagStatement = session.prepare("INSERT INTO osm_ways_bb_tag(bb, osmid, key, value, way) VALUES (?, ?, ?, ?, ?);")
  val insertRelationBBTagStatement = session.prepare("INSERT INTO osm_relations_bb_tag(bb, osmid, key, value, relation) VALUES (?, ?, ?, ?, ?);")


  def apply() = new OsmStorageService()
}

class OsmStorageService() {

  import io.plasmap.geo.data.OsmStorageService.{keyspaceConfig, _}

  import scala.collection.JavaConversions._

  val log = Logger(LoggerFactory.getLogger(OsmStorageService.getClass.getName))
  log.trace("Created OsmStorageService")

  private[this] def getBBTable(`type`: OsmType) = `type` match {
    case OsmTypeNode => "osm_nodes_bb"
    case OsmTypeWay => "osm_ways_bb"
    case OsmTypeRelation => "osm_relations_bb"
  }

  private[this] def getBBTagTable(`type`: OsmType) = `type` match {
    case OsmTypeNode => "osm_nodes_bb_tag"
    case OsmTypeWay => "osm_ways_bb_tag"
    case OsmTypeRelation => "osm_relations_bb_tag"
  }

  private[this] def getBoundingBox(r: Row) = {
    r.getLong("bb")
  }

  private[this] def getOsmId(r: Row) = {
    OsmId(r.getLong("osmId"))
  }

  private[this] def getKey(r: Row) = {
    r.getString("key")
  }

  private[this] def getValue(r: Row) = {
    r.getString("value")
  }

  private[this] def getNodeBytes(r: com.datastax.driver.core.Row) = {
    r.getBytes("node")
  }

  private[this] def getWayBytes(r: Row) = {
    r.getBytes("way")
  }

  private[this] def getRelationBytes(r: com.datastax.driver.core.Row) = {
    r.getBytes("relation")
  }

  private[this] def buildNodeBB(r: Row): Try[OsmBB] = {

    val bytes = getArray(getNodeBytes(r))
    fromBinary(bytes).map(OsmBB(getBoundingBox(r), getOsmId(r), _))
  }

  private[this] def buildWayBB(r: Row): Try[OsmBB] = {
    val bytes = getArray(getWayBytes(r))
    fromBinary(bytes).map(OsmBB(getBoundingBox(r), getOsmId(r), _))
  }

  private[this] def buildRelationBB(r: Row): Try[OsmBB] = {
    val bytes = getArray(getRelationBytes(r))
    fromBinary(bytes).map(OsmBB(getBoundingBox(r), getOsmId(r), _))
  }

  private[this] def buildNodeBBTag(r: Row): Try[OsmBBTag] = {
    val bytes = getArray(getNodeBytes(r))
    fromBinary(bytes).map(OsmBBTag(getBoundingBox(r), getOsmId(r), OsmTag(getKey(r), getValue(r)), _))
  }

  private[this] def buildWayBBTag(r: Row): Try[OsmBBTag] = {
    val bytes = getArray(getWayBytes(r))
    fromBinary(bytes).map(OsmBBTag(getBoundingBox(r), getOsmId(r), OsmTag(getKey(r), getValue(r)), _))
  }

  private[this] def buildRelationBBTag(r: Row): Try[OsmBBTag] = {
    val bytes = getArray(getRelationBytes(r))
    fromBinary(bytes).map(OsmBBTag(getBoundingBox(r), getOsmId(r), OsmTag(getKey(r), getValue(r)), _))
  }

  private[this] def buildElements[T](rs: ResultSet, buildElement: Row => T): List[T] = {
    rs.all().map(r => buildElement(r)).toList
  }

  def findBB(bb: Long, osmId: OsmId, typ: OsmType)(implicit ec: ExecutionContext): Future[Option[OsmBB]] = {

    val query = QueryBuilder
      .select()
      .all()
      .from(keyspaceConfig, getBBTable(typ))
      .where(QueryBuilder.eq("bb", bb))
      .and(QueryBuilder.eq("osmid", osmId.value))

    session.executeAsync(query) map ((r: ResultSet) => {

      r.all().headOption.flatMap((row) => {
        val element: Try[OsmBB] = typ match {
          case OsmTypeNode => buildNodeBB(row)
          case OsmTypeWay => buildWayBB(row)
          case OsmTypeRelation => buildRelationBB(row)
        }
        element.toOption
      })

    }

      )


  }

  def countBB(bb: Long, `type`: OsmType)(implicit ec: ExecutionContext): Future[Long] = {
    val query = QueryBuilder
      .select()
      .countAll()
      .from(keyspaceConfig, getBBTable(`type`))
      .where(QueryBuilder.eq("bb", bb))

    session.executeAsync(query) map (_.one.getLong(0))
  }

  def insertBB(elementBB: OsmBB)(implicit ec: ExecutionContext): Future[Option[OsmBB]] = {
    val insertStatement = elementBB match {
      case OsmBB(_, _, node: OsmDenormalizedNode) => insertNodeBBStatement
      case OsmBB(_, _, way: OsmDenormalizedWay) => insertWayBBStatement
      case OsmBB(_, _, relation: OsmDenormalizedRelation) => insertRelationBBStatement
    }

    val bb = elementBB.bb.asInstanceOf[AnyRef]
    val osmId = elementBB.osmId.value.asInstanceOf[AnyRef]
    val bytes = ByteBuffer.wrap(toBinary(elementBB.element))

    session.executeAsync(insertStatement.bind(bb, osmId, bytes)) map (_ => Some(elementBB))
  }

  def findSingleBBTag(bb: Long, osmId: OsmId, tag: OsmTag, typ: OsmType)(implicit ec: ExecutionContext): Future[Option[OsmBBTag]] = {
    log.trace(s"findSingleBBTag $bb $tag ${
      typ
    }")
    val query = QueryBuilder
      .select()
      .all()
      .from(keyspaceConfig, getBBTagTable(typ))
      .where(QueryBuilder.eq("bb", bb))
      .and(QueryBuilder.eq("osmid", osmId.value))
      .and(QueryBuilder.eq("key", tag.key))
      .and(QueryBuilder.eq("value", tag.value))


    session.executeAsync(query) map (r => {

      r.all().headOption.flatMap((row) => {
        val element: Try[OsmBBTag] = typ match {
          case OsmTypeNode => buildNodeBBTag(row)
          case OsmTypeWay => buildWayBBTag(row)
          case OsmTypeRelation => buildRelationBBTag(row)
        }
        element.toOption
      })
    }
      )

  }

  def findBBTag(bb: Long, tag: OsmTag, typ: OsmType)(implicit ec: ExecutionContext): Future[List[OsmBBTag]] = {
    log.trace(s"findBBTag $bb $tag $typ")
    val query = QueryBuilder
      .select()
      .all()
      .from(keyspaceConfig, getBBTagTable(typ))
      .where(QueryBuilder.eq("bb", bb))
      .and(QueryBuilder.eq("key", tag.key))
      .and(QueryBuilder.eq("value", tag.value))

    log.trace(s"Executing query ${
      query.toString
    }")

    session.executeAsync(query) map (r => {
      val elements: List[Try[OsmBBTag]] = buildElements(r, typ match {
        case OsmTypeNode => buildNodeBBTag
        case OsmTypeWay => buildWayBBTag
        case OsmTypeRelation => buildRelationBBTag
      })
      elements.flatMap(_.toOption)
    })

  }

  def countBBTag(bb: Long, tag: OsmTag, `type`: OsmType)(implicit ec: ExecutionContext): Future[Long] = {
    log.trace(s"findBBTag $bb $tag ${
      `type`
    }")
    val query = QueryBuilder
      .select()
      .countAll()
      .from(keyspaceConfig, getBBTagTable(`type`))
      .where(QueryBuilder.eq("bb", bb))
      .and(QueryBuilder.eq("key", tag.key))
      .and(QueryBuilder.eq("value", tag.value))

    session.executeAsync(query) map (_.one.getLong(0))
  }

  def insertBBTag(elementBBTag: OsmBBTag)(implicit ec: ExecutionContext): Future[Option[OsmBBTag]] = {

    val insertStatement = elementBBTag match {
      case OsmBBTag(_, _, tag, node: OsmDenormalizedNode) => insertNodeBBTagStatement
      case OsmBBTag(_, _, tag, way: OsmDenormalizedWay) => insertWayBBTagStatement
      case OsmBBTag(_, _, tag, relation: OsmDenormalizedRelation) => insertRelationBBTagStatement
    }

    val bb = elementBBTag.bb.asInstanceOf[AnyRef]
    val osmId = elementBBTag.osmId.value.asInstanceOf[AnyRef]
    val key = elementBBTag.tag.key.asInstanceOf[AnyRef]
    val value = elementBBTag.tag.value.asInstanceOf[AnyRef]
    val bytes = ByteBuffer.wrap(toBinary(elementBBTag.element))

    session.executeAsync(insertStatement.bind(bb, osmId, key, value, bytes)) map (_ => Some(elementBBTag))
  }

}
