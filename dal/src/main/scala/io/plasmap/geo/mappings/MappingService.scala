package io.plasmap.geo.mappings

import io.plasmap.geo.mappings.impl.MongoMappingService
import io.plasmap.model._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for inserting and retrieving osm object to bounding box mappings
  *
  * @author Jan Schulte <jan@plasmap.io>
 */
trait MappingService {

  def insertMappings(mappings:List[OsmMapping])(implicit ec: ExecutionContext): Future[List[Option[OsmMapping]]] = {
    Future.sequence(mappings.map(insertMapping))
  }

  def insertMapping(mapping: OsmMapping)(implicit ec: ExecutionContext): Future[Option[OsmMapping]] = mapping match {
    case nm: OsmNodeMapping => insertNodeMapping(nm)(ec)
    case wm: OsmWayMapping => insertWayMapping(wm)(ec)
    case rm: OsmRelationMapping => insertRelationMapping(rm)(ec)
  }

  def insertNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]]

  def insertWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]]

  def insertRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]]

  def findMapping(osmId: OsmId, typ: OsmType)(implicit ec: ExecutionContext):Future[Option[OsmMapping]] = typ match {
    case OsmTypeNode => findNodeMapping(osmId)
    case OsmTypeWay => findWayMapping(osmId)
    case OsmTypeRelation => findRelationMapping(osmId)
  }

  def findNodeMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]]

  def findWayMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]]

  def findRelationMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]]
}

object MappingService {

  def apply()(implicit ec:ExecutionContext):MappingService = MongoMappingService(ec)

}