package io.plasmap.geo.mappings.impl

import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings._
import io.plasmap.model._
import org.slf4j.LoggerFactory
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClient}

import scala.concurrent.{ExecutionContext, Future}


object RedisMappingService {

  private val config = ConfigFactory.load()

  private val hostConfig: String = config.getString("plasmap.db.redis.host")
  private val portConfig: Int = config.getInt("plasmap.db.redis.port")


  val log = Logger(LoggerFactory.getLogger(RedisMappingService.getClass.getName))
  log.info(s"${Console.GREEN}Created RedisMappingService[host=$hostConfig,port=$portConfig]${Console.RESET}")

  val system = akka.actor.ActorSystem()
  val redis = new RedisClient(hostConfig, portConfig)(system)

  def apply() = new RedisMappingService
}

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
class RedisMappingService extends MappingService {

  import MappingSerializer.{nodeMappingSerializer, relationMappingSerializer, wayMappingSerializer}
  import RedisMappingService.redis

  private val wayPrefix: String = "w"
  private val nodePrefix: String = "n"
  private val relationPrefix: String = "r"

  override def insertNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    redis.set(s"$nodePrefix${mapping.osmId.value}", mapping)(nodeMappingSerializer).map(if (_) {
      Some(mapping)
    } else None)
  }

  override def insertWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] = {
    redis.set(s"$wayPrefix${mapping.osmId.value}", mapping)(wayMappingSerializer).map(if (_) {
      Some(mapping)
    } else None)
  }

  override def insertRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] = {
    redis.set(s"$relationPrefix${mapping.osmId.value}", mapping)(relationMappingSerializer).map(if (_) {
      Some(mapping)
    } else None)
  }

  override def findNodeMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] = {
    redis.get(s"$nodePrefix${osmId.value}")(nodeMappingSerializer)
  }

  override def findWayMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] =
    redis.get(s"$wayPrefix${osmId.value}")(wayMappingSerializer)

  override def findRelationMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] =
    redis.get(s"$relationPrefix${osmId.value}")(relationMappingSerializer)

}
