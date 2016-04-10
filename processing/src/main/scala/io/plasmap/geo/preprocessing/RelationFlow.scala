package io.plasmap.geo.preprocessing

import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.Flow
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision.resumingDecider
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.data.{OsmStorageService, OsmBB}
import io.plasmap.geo.mappings._
import io.plasmap.model._
import io.plasmap.model.geometry._
import io.plasmap.util.Denormalizer
import io.plasmap.util.GeowUtils._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

/**
  * Created by mark on 30.10.15.
  */
case class RelationFlow(mappingService:MappingService, mappingEC:ExecutionContext,
                        storageService:OsmStorageService, dataEC:ExecutionContext,
                        log:Logger
                       )(implicit am:ActorMaterializer) {
  val filterRelation: Flow[OsmObject, OsmRelation, Unit] = Flow[OsmObject]
    .filter(isRelation)
    .map(_.asInstanceOf[OsmRelation])
    .log(s"PassingFilter", identity)

  val relFlow: Flow[OsmObject, FlowError \/ OsmDenormalizedRelation, Unit] = Flow[OsmObject]
    .via(filterRelation)
    .via(denormalizeRelationFlow())

  private def defaultMapRef(id:OsmId, typ:OsmType): Future[Option[OsmMapping]] = mappingService.findMapping(id,typ)(mappingEC)

  private def defaultToData(bb:Long, id:OsmId, typ:OsmType):Future[Option[OsmBB]] = storageService.findBB(bb,id,typ)(dataEC)


  /**
    * Creates the denormalization flow for relations.
    * @param mapRef Function to retrieve element to bounding box mapping
    * @param toData
    * @param denormalize
    * @return
    */
  def denormalizeRelationFlow(mapRef: (OsmId, OsmType) => Future[Option[OsmMapping]] = defaultMapRef,
                              toData: (Long, OsmId, OsmType) => Future[Option[OsmBB]] = defaultToData,
                              denormalize: (OsmRelation, Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection]) => OsmDenormalizedRelation = Denormalizer.denormalizeRelation): Flow[OsmRelation, FlowError \/ OsmDenormalizedRelation, Unit] = {

    type MemberId = (OsmType, OsmId)
    type RelationMapping = (OsmRelation, List[OsmBB])

    def foldZero(rel: OsmRelation): RelationMapping = rel -> List.empty[OsmBB]

    def fold(wm: RelationMapping, mapped: (OsmMember, Option[OsmBB])): RelationMapping = mapped match {
      case (nd, Some(osmBB)) =>
        val (relation, acc) = wm
        val newList: List[OsmBB] = osmBB :: acc
        relation -> newList: RelationMapping
      case (nd, None) =>
        wm: RelationMapping
    }

    def extractType(ref: OsmMember): (OsmId, OsmType) = ref.ref -> ref.typ

    def extractMapping(mapping: OsmMapping): (Long, OsmId, OsmType) = mapping match {
      case OsmNodeMapping    (hash, id, _) => (hash, id, OsmTypeNode)
      case OsmWayMapping     (hash, id, _) => (hash, id, OsmTypeWay)
      case OsmRelationMapping(hash, id, _) => (hash, id, OsmTypeRelation)
    }

    val getData: ((OsmMember, Option[OsmMapping])) => Future[(OsmMember, Option[OsmBB])] = (tuple) => {
      val (member, mappingOpt) = tuple
      mappingOpt match {
        case Some(mapping) =>
          toData.tupled(extractMapping(mapping)).map(member -> _)(dataEC)
        case None =>
          Future { member -> None }(dataEC)
      }
    }

    val getMapping: (OsmMember) => Future[(OsmMember, Option[OsmMapping])] = (ref) => {
      mapRef.tupled(extractType(ref)).map(ref -> _)(mappingEC)
    }

    val membersFlow: Flow[OsmRelation, (OsmRelation, List[OsmBB]), Unit] =
      Flow[OsmRelation]
        .mapConcat((relation) => relation.refs.map(relation -> _))
        .groupBy{ case (relation, ref) => relation }
        .map {
          case (relation, groupStream) =>
              groupStream
              .take(relation.refs.size)
              .map(_._2)
              .mapAsync(4)(getMapping)
              .withAttributes(supervisionStrategy(resumingDecider))
              .log(s"MappedRef", identity)
              .map((mappingResult) => {
                val (member, mappingOpt) = mappingResult
                mappingOpt match {
                  case Some(osmBB) => log.trace(s"Found data for member $member")
                  case None        => log.warn(s"issuing data for member $member")
                }
                mappingResult
              })
              .mapAsync(4)(getData)
              .withAttributes(supervisionStrategy(resumingDecider))
              .log(s"DataRef", identity)
              .map((dataResult) => {
                val (member, osmBBOpt) = dataResult
                osmBBOpt match {
                  case Some(_) => log.trace(s"Found data for member $member")
                  case None    => log.warn (s"Missing data for member $member")
                }
                dataResult
              })
              .runFold(foldZero(relation))(fold)

        }
        .log("Member", identity)
        .buffer(1024, OverflowStrategy.dropHead)
        .mapAsync(1)(identity)

    def extractGeometries(relation: OsmRelation, list: List[OsmBB]): (OsmRelation, Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection]) = {
      val nodeBB: List[(OsmId, Point)] = list.collect { case OsmBB(_, nodeId, node: OsmDenormalizedNode) => nodeId -> node.geometry }
      val wayBB: List[(OsmId, LineString)] = list.collect { case OsmBB(_, wayId, way: OsmDenormalizedWay) => wayId -> way.geometry }
      val relationBB: List[(OsmId, GeometryCollection)] = list.collect { case OsmBB(_, subRelId, subRel: OsmDenormalizedRelation) => subRelId -> subRel.geometry }

      (relation, nodeBB.toMap, wayBB.toMap, relationBB.toMap)
    }

    val flow: Flow[OsmRelation, FlowError \/ OsmDenormalizedRelation, Unit] = membersFlow
      .map((extractGeometries _).tupled)
      .map((quadruple) => {
        val (rel,nodeMembers,wayMembers,relMembers) = quadruple
        val intent = \/.fromTryCatchNonFatal(
          denormalize(rel, nodeMembers, wayMembers, relMembers)
        ).leftMap((t: Throwable) => CouldNotDenormaliseObject(rel, t))
        intent
      })
      .withAttributes(supervisionStrategy(resumingDecider))
      .log(s"DenormalizedRelation", identity)
    flow
  }

}
