package io.plasmap.geo.mappings.impl

import com.github.mauricio.async.db.RowData
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings.impl.mysql.{DatabaseFactory, MySQLDatabase}
import io.plasmap.geo.mappings._
import io.plasmap.model._
import org.joda.time.{DateTime, LocalDateTime}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object MySQLMappingService {

  private val config = ConfigFactory.load()

  private val hostConfig: String = config.getString("plasmap.db.mysql.host")
  private val portConfig: Int = config.getInt("plasmap.db.mysql.port")
  private val usernameConfig: String = config.getString("plasmap.db.mysql.username")
  private val passwordConfig: String = config.getString("plasmap.db.mysql.password")
  private val databaseConfig: String = config.getString("plasmap.db.mysql.database")

  private val db = DatabaseFactory.newDatabase(
    host = hostConfig,
    port = portConfig,
    username = usernameConfig,
    password = Some(passwordConfig),
    database = Some(databaseConfig))


  val log = Logger(LoggerFactory.getLogger(MySQLMappingService.getClass.getName))
  log.info(s"Created OsmMappingService for $hostConfig:$portConfig")

  def apply() = new MySQLMappingService(db)
}

object TableNames {
  val OsmNodeMapping = "osm_node_mapping"
  val OsmWayMapping = "osm_way_mapping"
  val OsmRelationMapping = "osm_relation_mapping"
}

class MySQLMappingService(db: MySQLDatabase) extends MappingService {

  override def insertMapping(mapping:OsmMapping)(implicit ec: ExecutionContext):Future[Option[OsmMapping]] = mapping match {
    case OsmNodeMapping(bb,osmId,updated) => insertMapping(mapping,TableNames.OsmNodeMapping)
    case OsmWayMapping(bb,osmId,updated) => insertMapping(mapping,TableNames.OsmWayMapping)
    case OsmRelationMapping(bb,osmId,updated) => insertMapping(mapping,TableNames.OsmRelationMapping)
  }

  private def insertMapping[T <: OsmMapping](mapping: T, tableName: String)(implicit ec: ExecutionContext): Future[Option[T]] = {
    val future = db.execute(s"REPLACE INTO $tableName (osm_id, hash, updated) VALUES (?,?,?);", mapping.osmId.value, mapping.hash, mapping.updated)
    future.map(qr => if(qr.rowsAffected > 0){ Some(mapping) } else None )
  }

  override def insertNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] =
    insertMapping(mapping, TableNames.OsmNodeMapping)

  override def insertWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] =
    insertMapping(mapping, TableNames.OsmWayMapping)

  override def insertRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] =
    insertMapping(mapping, TableNames.OsmRelationMapping)

  override def findMapping(osmId:OsmId,typ:OsmType)(implicit ec: ExecutionContext):Future[Option[OsmMapping]]  = typ match {
    case OsmTypeNode => findNodeMapping(osmId)
    case OsmTypeWay => findWayMapping(osmId)
    case OsmTypeRelation => findRelationMapping(osmId)
  }

  private def findMapping[A <: OsmMapping](osmId: OsmId, tableName:String, convert: (Long, Long, DateTime) ⇒ A)(implicit ec: ExecutionContext): Future[Option[A]] = {
    val future = db.fetch(s"SELECT osm_id, hash, updated FROM $tableName WHERE osm_id = ?;", osmId.value)
    future.map {
      case Some(rowData) if rowData.nonEmpty =>
        val head: RowData = rowData.head
        val bb = head("hash").asInstanceOf[Long]
        val osmId = head("osm_id").asInstanceOf[Long]
        val updated = head("updated").asInstanceOf[LocalDateTime].toDateTime
        Some(convert(bb, osmId, updated))
      case _ => None
    }
  }

  override def findNodeMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    findMapping(osmId, TableNames.OsmNodeMapping, (bb, id, updated) ⇒ OsmNodeMapping(bb, OsmId(id),updated))
  }

  override def findWayMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {
    findMapping(osmId, TableNames.OsmWayMapping, (bb, id, updated) ⇒ OsmWayMapping( bb, OsmId(id),updated))
  }

  override def findRelationMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {
    findMapping(osmId, TableNames.OsmRelationMapping, (bb, id, updated) ⇒ OsmRelationMapping( bb,OsmId(id), updated))
  }
}