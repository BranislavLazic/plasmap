package io.plasmap.geo.preprocessing

import java.io.{PrintWriter, StringWriter}

import akka.stream.{Materializer, FlowShape, Graph}
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.data.{OsmBB, OsmStorageService}
import io.plasmap.geo.mappings._
import io.plasmap.geo.preprocessing.UtilityFlows._
import io.plasmap.model._
import io.plasmap.model.geometry.{Geometry, GeometryCollection, LineString, Point}
import io.plasmap.util.Denormalizer
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

/**
 * Created by janschulte on 01/03/16.
 */
case class RelationDenormalizer(ec:ExecutionContext,mat:Materializer) {

  val log = Logger(LoggerFactory.getLogger(RelationDenormalizer.getClass.getName))

  implicit val context = ec

  lazy val mappingService = MappingService()
  lazy val storageService = OsmStorageService()


  private[this] def defaultMapRef(id: OsmId, typ: OsmType): Future[Option[OsmMapping]] = mappingService.findMapping(id, typ)(ec)
  private[this] def defaultToData(bb: Long, id: OsmId, typ: OsmType): Future[Option[OsmBB]] = storageService.findBB(bb, id, typ)(ec)

  private[this] def foldZero = List.empty[(OsmId, OsmType, Geometry)]

  private[this] def fold(acc: List[(OsmId, OsmType, Geometry)], mapped: Option[(OsmId, OsmType, Geometry)]): List[(OsmId, OsmType, Geometry)] = mapped match {
    case Some(osmBB) =>
      osmBB :: acc
    case None =>
      acc
  }

  private[this] def extractType(ref: OsmMember): (OsmId, OsmType) = ref.ref -> ref.typ

  private[this] def extractMapping(mapping: OsmMapping): (Long, OsmId, OsmType) = mapping match {
    case OsmNodeMapping(hash, id, _) => (hash, id, OsmTypeNode)
    case OsmWayMapping(hash, id, _) => (hash, id, OsmTypeWay)
    case OsmRelationMapping(hash, id, _) => (hash, id, OsmTypeRelation)
  }

  private[this] def extractGeometries(relation: OsmRelation, list: List[(OsmId, OsmType, Geometry)]): (OsmRelation, Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection]) = {
    val nodeBB: List[(OsmId, Point)] = list.collect { case (id, OsmTypeNode, geometry: Point) => id -> geometry }
    val wayBB: List[(OsmId, LineString)] = list.collect { case (id, OsmTypeWay, geometry: LineString) => id -> geometry }
    val relationBB: List[(OsmId, GeometryCollection)] = list.collect { case (id, OsmTypeRelation, geometry: GeometryCollection) => id -> geometry }

    (relation, nodeBB.toMap, wayBB.toMap, relationBB.toMap)
  }
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

    Flow[OsmRelation]
      .mapAsync(16)((relation) => {

        val memberFlow: Flow[OsmRelation, Option[(OsmId, OsmType, Geometry)], Unit] =
          Flow[OsmRelation]
            .mapConcat(_.refs)
            .mapAsync(32)((ref) => {

              mapRef(ref.ref, ref.typ)
                .recover { case e: Throwable =>
                  log.error(s"Failed to get mapping for ref $ref, error $e")
                  None
                }.map(ref -> _)
            })
            .filter(_._2.isDefined)
            .map((tuple) => tuple._1 -> tuple._2.get)
            .mapAsync(32)((tuple: (OsmMember, OsmMapping)) => {
              val (ref, mapping) = tuple
              val mappingTuple: (Long, OsmId, OsmType) = extractMapping(mapping)
              val (bb, id, typ) = mappingTuple

              val eventualProduct: Future[Option[(OsmId, OsmType, Geometry)]] = typ match {
                case OsmTypeNode =>
                  Future {
                    Some((ref.ref, typ, Point(bb)))
                  }
                case _ =>
                  toData.tupled(mappingTuple)
                    .recover {
                      case e: Throwable =>

                        val sw = new StringWriter
                        e.printStackTrace(new PrintWriter(sw))

                        log.error(
                          s"""Failed to unpickle data for ref $ref.
                             | Msg: ${e.getMessage}
                             | Stacktrace: ${sw.toString}""".stripMargin)
                        None
                    }.map((opt) => opt.map((osmBB) => {
                    (ref.ref, typ, osmBB.element.geometry)
                  }))
              }
              eventualProduct
            })

        Source
          .single(relation)
          .via(memberFlow)
          .runFold(foldZero)(fold)(mat)
          .map((triple) => extractGeometries(relation, triple))(ec)
          .map((quadtruple: (OsmRelation, Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection])) => {
            val (rel, nodeMembers, wayMembers, relMembers) = quadtruple
            val intent: \/[CouldNotDenormaliseObject, OsmDenormalizedRelation] = \/.fromTryCatchNonFatal(
              denormalize(rel, nodeMembers, wayMembers, relMembers)
            ).leftMap(t => {
              CouldNotDenormaliseObject(rel,  t)
            })
            intent
          })(ec)
      })
  }

  val flow = Flow[OsmObject]
    .via(filterRelation)
    .via(denormalizeRelationFlow())
}
