package io.plasmap.geo.preprocessing

import akka.NotUsed
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision.resumingDecider
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.data.{OsmBB, OsmStorageService}
import io.plasmap.geo.mappings._
import io.plasmap.model._
import io.plasmap.model.geometry._
import io.plasmap.util.Denormalizer
import io.plasmap.util.GeowUtils._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

object RelationFlow {

  /**
    * Creates the denormalization flow for relations.
    *
    * @param mapRef Function to retrieve element to bounding box mapping
    * @param toData
    * @param denormalizeRelation
    * @return
    */
  def denormalizeRelationFlow(mapRef: (OsmId, OsmType) => Future[Option[OsmMapping]],
                              toData: (Long, OsmId, OsmType) => Future[Option[OsmBB]],
                              denormalizeRelation: (OsmRelation, Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection]) => OsmDenormalizedRelation = Denormalizer.denormalizeRelation)
                             (implicit ec:ExecutionContext, mat:Materializer)
  : Flow[OsmRelation, FlowError \/ OsmDenormalizedRelation, NotUsed] = {

    def extractType(ref: OsmMember): (OsmId, OsmType) = ref.ref -> ref.typ

    def extractMapping(mapping: OsmMapping): (Long, OsmId, OsmType) = mapping match {
      case OsmNodeMapping(hash, id, _) => (hash, id, OsmTypeNode)
      case OsmWayMapping(hash, id, _) => (hash, id, OsmTypeWay)
      case OsmRelationMapping(hash, id, _) => (hash, id, OsmTypeRelation)
    }

    def extractGeometries(relation: OsmRelation)(list: List[OsmBB]): (Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection]) = {
      val nodeBB: List[(OsmId, Point)] = list.collect { case OsmBB(_, nodeId, node: OsmDenormalizedNode) => nodeId -> node.geometry }
      val wayBB: List[(OsmId, LineString)] = list.collect { case OsmBB(_, wayId, way: OsmDenormalizedWay) => wayId -> way.geometry }
      val relationBB: List[(OsmId, GeometryCollection)] = list.collect { case OsmBB(_, subRelId, subRel: OsmDenormalizedRelation) => subRelId -> subRel.geometry }

      (nodeBB.toMap, wayBB.toMap, relationBB.toMap)
    }

    def denormalise(rel: OsmRelation)(
      nodeMembers: Map[OsmId, Point],
      wayMembers: Map[OsmId, LineString],
      relMembers: Map[OsmId, GeometryCollection]): \/[CouldNotDenormaliseObject, OsmDenormalizedRelation] = {
      \/.fromTryCatchNonFatal(
        denormalizeRelation(rel, nodeMembers, wayMembers, relMembers)
      ).leftMap((t: Throwable) => CouldNotDenormaliseObject(rel, t))
    }


    Flow[OsmRelation].mapAsync(4)(relation =>
      Source.single(relation)
        .mapConcat(_.refs)
        .map(extractType)
        .mapAsync(4)(mapRef.tupled)
        .filter(_.isDefined)
        .map(_.get)
        .map(extractMapping)
        .mapAsync(4)(toData.tupled)
        .filter(_.isDefined)
        .map(_.get)
        .runFold(List.empty[OsmBB])((acc, osmBB) => osmBB :: acc)
        .map(extractGeometries(relation)(_))
        .map((denormalise(relation)(_,_,_)).tupled)
    )
  }

}
