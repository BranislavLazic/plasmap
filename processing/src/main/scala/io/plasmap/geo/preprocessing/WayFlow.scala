package io.plasmap.geo.preprocessing

import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Source, Flow}
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings.{MappingService, OsmNodeMapping}
import akka.stream.Supervision.resumingDecider
import io.plasmap.model._
import io.plasmap.model.geometry.Point
import io.plasmap.util.Denormalizer
import akka.stream.ActorAttributes.supervisionStrategy
import io.plasmap.util.GeowUtils._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/

/**
  * Created by mark on 30.10.15.
  */
case class WayFlow(mappingService: MappingService,
                  mappingEC:ExecutionContext,
                  log:Logger)(implicit am:ActorMaterializer) {
  val filterWay: Flow[OsmObject, OsmWay, Unit] = Flow[OsmObject]
    .filter(isWay)
    .map(_.asInstanceOf[OsmWay])
    .log(s"PassingFilter", identity)

  val wayFlow: Flow[OsmObject, FlowError \/ OsmDenormalizedWay, Unit] = Flow[OsmObject]
    .via(filterWay)
    .via(denormalizeWayFlow())

  private def defaultMapNd(id:OsmId):Future[Option[OsmNodeMapping]] = mappingService.findNodeMapping(id)(mappingEC)

  /**
    * Creates the denormalization flow for ways.
    * @param mapNd Function to retrieve node to hash mapping
    * @param denormalizeWay Function to denormalize a way
    * @return The flow
    */
  def denormalizeWayFlow(
                          mapNd: (OsmId) => Future[Option[OsmNodeMapping]] = defaultMapNd,
                          denormalizeWay: (OsmWay, Map[OsmId, Point]) => OsmDenormalizedWay = Denormalizer.denormalizeWay
                        ):
  Flow[OsmWay, FlowError \/ OsmDenormalizedWay, Unit] = {

    val toNdsFlow: Flow[OsmWay, (OsmWay, OsmId), Unit] = Flow[OsmWay].mapConcat((way) => way.nds.map(way -> _))

    type WayMapping = (OsmWay, Map[OsmId, Point])

    def foldZero(way: OsmWay): WayMapping = way -> Map.empty[OsmId, Point]

    def fold(wm: WayMapping, mapped: (OsmId, Option[OsmNodeMapping])): WayMapping = mapped match {
      case (nd, Some(OsmNodeMapping(hash, osmId, _))) =>
        val (way, oldMap)  = wm
        val mapping        = osmId -> Point(hash)
        val newMap         = oldMap + mapping
        way -> newMap: WayMapping
      case (nd, None) =>
        log.error(s"Missing mapping for nd $nd")
        wm: WayMapping
    }

    val accMappedFlow: Flow[OsmWay, Future[(OsmWay, Map[OsmId, Point])], Unit] = toNdsFlow.groupBy(_._1).map {
      case (way, groupStream) =>
        val mappedSource: Source[(OsmId, Option[OsmNodeMapping]), Unit] =
          groupStream
            .take(way.nds.size)
            .map(_._2)
            .mapAsync(4)((nd) => {
              implicit val ec = mappingEC
              mapNd(nd)
                .recover { case e: Exception =>
                  log.warn(s"Failed to get mapping for nd $nd, error $e")
                  None
                }
                .map(nd -> _)
            })
            .withAttributes(supervisionStrategy(resumingDecider))

        mappedSource.runFold(foldZero(way))(fold)
    }.log("MappedNds", identity)

    accMappedFlow
      .buffer(1024, OverflowStrategy.dropHead)
      .mapAsync(1)(identity)
      .log(s"MappedWay", identity)
      .map( tuple => {
        val (way,nds) = tuple
        val intent: FlowError \/ OsmDenormalizedWay =
          \/.fromTryCatchNonFatal(denormalizeWay(way,nds))
            .leftMap(t => CouldNotDenormaliseObject(way, t))
        intent
      })
      .log(s"DenormalizedWay", identity)
  }

}
