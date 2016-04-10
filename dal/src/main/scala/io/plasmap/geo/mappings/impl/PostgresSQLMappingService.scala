package io.plasmap.geo.mappings.impl

import io.plasmap.geo.mappings._
import io.plasmap.model.{OsmType, OsmId}
import org.joda.time.DateTime
import slick.dbio
import slick.dbio.Effect.Write
import slick.lifted.Tag
import slick.driver.PostgresDriver.api._
import slick.profile.{SqlAction, FixedSqlAction}

import scala.concurrent.{Future, ExecutionContext}

case class NodeMappings(tag: Tag) extends Table[(Long, Long, Long)](tag, "node_mappings") {

  def id = column[Long]("osm_id", O.PrimaryKey)

  def hash = column[Long]("hash")

  def ts = column[Long]("ts")

  def * = (id, hash, ts)
}


case class WayMappings(tag: Tag) extends Table[(Long, Long, Long)](tag, "way_mappings") {

  def id = column[Long]("osm_id", O.PrimaryKey)

  def hash = column[Long]("hash")

  def ts = column[Long]("ts")

  def * = (id, hash, ts)
}


case class RelationMappings(tag: Tag) extends Table[(Long, Long, Long)](tag, "relation_mappings") {

  def id = column[Long]("osm_id", O.PrimaryKey)

  def hash = column[Long]("hash")

  def ts = column[Long]("ts")

  def * = (id, hash, ts)
}


/**
 * @author Jan Schulte <jan@plasmap.io>
 */
class PostgresSQLMappingService() extends MappingService {

  import PostgresSQLMappingService._

  private[this] def rowsAffectedToOption[T <: OsmMapping](mapping: T): (Int) => Option[T] = {
    (rowsUpdated) => {
      if (rowsUpdated == 0) {
        None
      } else {
        Some(mapping)
      }
    }
  }

  /*private[this] def nodeMappingUpsert(nm: OsmNodeMapping): DBIO[Int] = {
    val osmId = nm.osmId.value
    val hash = nm.hash
    val ts = nm.updated.getMillis
    sqlu"insert into node_mappings values (#$osmId, #$hash, #$ts) ON CONFLICT UPDATE SET hash=#$hash , ts=#$ts"
  }

  private[this] def wayMappingUpsert(wm: OsmWayMapping): DBIO[Int] = {
    val osmId = wm.osmId.value
    val hash = wm.hash
    val ts = wm.updated.getMillis
    sql"insert into way_mappings values (#$osmId, #$hash, #$ts}) ON CONFLICT UPDATE SET hash=#$hash , ts=#$ts"
  }

  private[this] def relationMappingUpsert(rm: OsmRelationMapping): DBIO[Int] = {
    val osmId = rm.osmId.value
    val hash = rm.hash
    val ts = rm.updated.getMillis

    sqlu"insert into relation_mappings values (#$osmId, #$hash, #$ts) ON CONFLICT UPDATE SET hash=#$hash , ts=#$ts"
  }*/

  private[this] def nodeMappingUpsert(triple:(Long,Long,Long)) = {
    val (osm_id, hash, ts) = triple
    // see this article http://www.the-art-of-web.com/sql/upsert/
    val insert     = s"INSERT INTO node_mappings (osm_id, hash, ts) SELECT '$osm_id','$hash','$ts'"
    val upsert     = s"UPDATE node_mappings SET hash=$hash, ts=$ts WHERE osm_id='$osm_id' "
    val finalStmnt = s"WITH upsert AS ($upsert RETURNING *) $insert WHERE NOT EXISTS (SELECT * FROM upsert)"
    sqlu"#$finalStmnt"
  }

  override def insertMappings(mappings: List[OsmMapping])(implicit ec: ExecutionContext): Future[List[Option[OsmMapping]]] = {
    val nms = mappings.collect { case nm: OsmNodeMapping => nm }.map(toTriple).map(nodeMappingUpsert)
    //val wms = mappings.collect { case wm: OsmWayMapping => wm }.map(toTriple).map(wayMappings.insertOrUpdate)
    //val rms = mappings.collect { case rm: OsmRelationMapping => rm }.map(toTriple).map(relationMappings.insertOrUpdate)

    //nodeMappings.forceInsertAll()

    db.run(DBIO.sequence(nms)).map(_.sum)
      .map((sum) =>
        if (sum > 0) {
          mappings.map(Some(_))
        } else {
          mappings.map(_ => None)
        })
  }

  override def insertNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {

    val updateAction = nodeMappings.insertOrUpdate(toTriple(mapping))
    db.run(updateAction).map(rowsAffectedToOption(mapping))

  }

  def toTriple(mapping: OsmNodeMapping): (Long, Long, Long) = {
    (mapping.osmId.value, mapping.hash, mapping.updated.getMillis)
  }

  private[impl] def deleteNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    val q = nodeMappings.filter(_.id === mapping.osmId.value)
    val action = q.delete

    db.run(action).map(rowsAffectedToOption(mapping))
  }

  override def insertWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {

    val updateAction = wayMappings.insertOrUpdate(toTriple(mapping))
    db.run(updateAction).map(rowsAffectedToOption(mapping))

  }

  def toTriple(mapping: OsmWayMapping): (Long, Long, Long) = {
    (mapping.osmId.value, mapping.hash, mapping.updated.getMillis)
  }

  private[impl] def deleteWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {

    val action = wayMappings.filter(_.id === mapping.osmId.value).delete
    db.run(action).map(rowsAffectedToOption(mapping))

  }

  override def insertRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {

    val updateAction = relationMappings.insertOrUpdate(toTriple(mapping))
    db.run(updateAction).map(rowsAffectedToOption(mapping))

  }

  def toTriple(mapping: OsmRelationMapping): (Long, Long, Long) = {
    (mapping.osmId.value, mapping.hash, mapping.updated.getMillis)
  }

  private[impl] def deleteRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {

    val action = relationMappings.filter(_.id === mapping.osmId.value).delete
    db.run(action).map(rowsAffectedToOption(mapping))

  }


  override def findNodeMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {

    val q = for {
      nm <- nodeMappings if nm.id === osmId.value
    } yield nm

    db.run(q.result)
      .map(_.headOption)
      .map(_.map((tuple) => {
        val (id, hash, ts) = tuple
        OsmNodeMapping(hash, OsmId(id), new DateTime(ts))
      }))
  }

  override def findWayMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {

    val q = for {
      wm <- wayMappings if wm.id === osmId.value
    } yield wm

    db.run(q.result)
      .map(_.headOption)
      .map(_.map((tuple) => {
        val (id, hash, ts) = tuple
        OsmWayMapping(hash, OsmId(id), new DateTime(ts))
      }))
  }

  override def findRelationMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {

    val q = for {
      rm <- relationMappings if rm.id === osmId.value
    } yield rm

    db.run(q.result)
      .map(_.headOption)
      .map(_.map((tuple) => {
        val (id, hash, ts) = tuple
        OsmRelationMapping(hash, OsmId(id), new DateTime(ts))
      }))
  }

}

object PostgresSQLMappingService {

  val nodeMappings = TableQuery[NodeMappings]
  val nodeMappingsCmp = Compiled(TableQuery[NodeMappings])
  val wayMappings = TableQuery[WayMappings]
  val relationMappings = TableQuery[RelationMappings]

  //val db = Database.forConfig("plasmap.db.postgres")
  val db = Database.forURL("jdbc:postgresql://postgres.weave.local/plasmap", "plasmap", "plasmap", driver = "org.postgresql.Driver",
    executor = AsyncExecutor("mapping", numThreads = 64, queueSize = 5000))

  def apply() = new PostgresSQLMappingService()
}
