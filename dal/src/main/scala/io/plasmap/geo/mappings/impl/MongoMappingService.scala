package io.plasmap.geo.mappings.impl

import com.typesafe.config.ConfigFactory
import io.plasmap.geo.mappings.{OsmWayMapping, OsmRelationMapping, OsmNodeMapping, MappingService}
import io.plasmap.model.OsmId
import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{MongoConnection, DefaultDB, MongoDriver}
import reactivemongo.bson._
import reactivemongo.core.nodeset.Authenticate

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by janschulte on 09/12/15.
 */
object MongoMappingService {

  private val config = ConfigFactory.load()

  private val host = config.getString("plasmap.db.mongo.host")

  private val database = config.getString("plasmap.db.mongo.database")
  private val username = config.getString("plasmap.db.mongo.user")
  private val password = config.getString("plasmap.db.mongo.password")

  private val credentials = List(Authenticate(database, username, password))

  private val driver = new MongoDriver
  private val connection: MongoConnection = driver.connection(List(host), authentications = credentials)

  private val FIELD_ID = "_id"
  private val FIELD_HASH = "hash"
  private val FIELD_TS = "ts"

  private def getTimestamp(doc: BSONDocument): Long = {
    doc.getAs[BSONDateTime](FIELD_TS).get.value
  }

  private def getHash(doc: BSONDocument): Long = {
    doc.getAs[BSONLong](FIELD_HASH).get.value
  }

  private def getOsmId(doc: BSONDocument): Long = {
    doc.getAs[BSONLong](FIELD_ID).get.value
  }

  implicit object OsmNodeMappingBSONReader extends BSONDocumentReader[OsmNodeMapping] {
    override def read(document: BSONDocument) :OsmNodeMapping = {

      val osmId = getOsmId(document)
      val hash = getHash(document)
      val ts = getTimestamp(document)

      OsmNodeMapping(hash,OsmId(osmId), new DateTime(ts))
    }
  }

  implicit object OsmNodeMappingBSONWriter extends BSONDocumentWriter[OsmNodeMapping] {
    override def write(mapping: OsmNodeMapping) = {
      BSONDocument(
        FIELD_ID -> BSONLong(mapping.osmId.value),
        FIELD_HASH -> BSONLong(mapping.hash),
        FIELD_TS -> BSONDateTime(mapping.updated.getMillis))
    }
  }

  implicit object OsmWayMappingBSONReader extends BSONDocumentReader[OsmWayMapping] {
    override def read(document: BSONDocument) :OsmWayMapping = {

      val osmId = getOsmId(document)
      val hash = getHash(document)
      val ts = getTimestamp(document)

      OsmWayMapping(hash,OsmId(osmId), new DateTime(ts))
    }
  }

  implicit object OsmWayMappingBSONWriter extends BSONDocumentWriter[OsmWayMapping] {
    override def write(mapping: OsmWayMapping) = {
      BSONDocument(
        FIELD_ID -> BSONLong(mapping.osmId.value),
        FIELD_HASH -> BSONLong(mapping.hash),
        FIELD_TS -> BSONDateTime(mapping.updated.getMillis))
    }
  }

  implicit object OsmRelationMappingBSONReader extends BSONDocumentReader[OsmRelationMapping] {
    override def read(document: BSONDocument) :OsmRelationMapping = {

      val osmId = getOsmId(document)
      val hash = getHash(document)
      val ts = getTimestamp(document)

      OsmRelationMapping(hash,OsmId(osmId), new DateTime(ts))
    }
  }

  implicit object OsmRelationMappingBSONWriter extends BSONDocumentWriter[OsmRelationMapping] {
    override def write(mapping: OsmRelationMapping) = {
      BSONDocument(
        FIELD_ID -> BSONLong(mapping.osmId.value),
        FIELD_HASH -> BSONLong(mapping.hash),
        FIELD_TS -> BSONDateTime(mapping.updated.getMillis))
    }
  }

  def apply(ec: ExecutionContext) = new MongoMappingService(connection,ec)
}

class MongoMappingService(conn:MongoConnection,ec: ExecutionContext) extends MappingService {

  val db: DefaultDB = conn("plasmap")(ec)

  val nodeMappings: BSONCollection = db("node_mappings")
  val wayMappings: BSONCollection = db("way_mappings")
  val relationMappings: BSONCollection = db("relation_mappings")

  import MongoMappingService._

  override def findNodeMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    val query = BSONDocument(FIELD_ID -> osmId.value)
    nodeMappings.find(query).one[OsmNodeMapping]
  }

  private[impl] def deleteNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    val selector = BSONDocument(
      FIELD_ID -> BSONLong(mapping.osmId.value))
    nodeMappings.remove(selector).map((result) => if (result.ok) {
      Some(mapping)
    } else {
      None
    })
  }

  override def insertNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    nodeMappings.insert(mapping).map((result) => if (result.ok) {
      Some(mapping)
    } else {
      None
    })
  }

  override def findWayMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {
    val query = BSONDocument(FIELD_ID -> osmId.value)
    wayMappings.find(query).one[OsmWayMapping]
  }

  private[impl] def deleteWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {
    val selector = BSONDocument(
      FIELD_ID -> BSONLong(mapping.osmId.value))
    wayMappings.remove(selector).map((result) => if (result.ok) {
      Some(mapping)
    } else {
      None
    })
  }

  override def insertWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {
    wayMappings.insert(mapping).map((result) => if (result.ok) {
      Some(mapping)
    } else {
      None
    })
  }

  override def insertRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {
    relationMappings.insert(mapping).map((result) => if (result.ok) {
      Some(mapping)
    } else {
      None
    })
  }

  override def findRelationMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {
    val query = BSONDocument(FIELD_ID -> osmId.value)
    relationMappings.find(query).one[OsmRelationMapping]
  }

  private[impl] def deleteRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {
    val selector = BSONDocument(
      FIELD_ID -> BSONLong(mapping.osmId.value))
      relationMappings.remove(selector).map((result) => if (result.ok) {
      Some(mapping)
    } else {
      None
    })
  }
}