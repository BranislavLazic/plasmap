package io.plasmap.geo.preprocessing

import java.io.{PrintWriter, StringWriter}

import akka.stream.Materializer
import akka.stream.scaladsl.{Source, Flow}
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings.{MappingService, OsmNodeMapping}
import io.plasmap.geo.preprocessing.UtilityFlows._
import io.plasmap.model.geometry.Point
import io.plasmap.model.{OsmObject, OsmDenormalizedWay, OsmWay, OsmId}
import io.plasmap.util.Denormalizer
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, ExecutionContext}
import scalaz.\/

/**
 * Created by janschulte on 01/03/16.
 */
case class WayDenormalizer(ec:ExecutionContext,mat:Materializer) {

  val log = Logger(LoggerFactory.getLogger(WayDenormalizer.getClass.getName))

  lazy val mappingService = MappingService()(ec)
  private val defaultMapNd = (id: OsmId) => mappingService.findNodeMapping(id)(ec)

  def foldZero = Map.empty[OsmId, Point]

  def fold(acc: Map[OsmId, Point], mapped: (OsmId, Option[OsmNodeMapping])): Map[OsmId, Point] = mapped match {
    case (nd, Some(OsmNodeMapping(hash, osmId, _))) =>
      val mapping = osmId -> Point(hash)
      acc + mapping
    case (nd, None) =>
      acc
  }

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

    val flow = Flow[OsmWay].mapAsync(16)((way) => {

      val mappingFlow: Flow[OsmWay, (OsmId, Option[OsmNodeMapping]), Unit] =
        Flow[OsmWay]
          .mapConcat(_.nds)
          .mapAsync(32)((nd) => {

            mapNd(nd)
              .recover { case e: Throwable =>
                val sw = new StringWriter
                e.printStackTrace(new PrintWriter(sw))

                log.error(
                  s"""Failed to get mapping for nd $nd.
                     | Msg: ${e.getMessage}
                     | Stacktrace: ${sw.toString}""".stripMargin)
                None
              }(ec)
              .map((mapping) => nd -> mapping)(ec)
          })

      Source.single(way)
        .via(mappingFlow)
        .runFold(foldZero)(fold)(mat)
        .map((nds: Map[OsmId, Point]) => {
          val intent: FlowError \/ OsmDenormalizedWay =
            \/.fromTryCatchNonFatal(denormalizeWay(way, nds)).leftMap(t => {
              CouldNotDenormaliseObject(way, t)
            })
          intent
        })(ec)

    })

    flow
  }

  lazy val flow: Flow[OsmObject, FlowError \/ OsmDenormalizedWay, Unit] = Flow[OsmObject]
    .via(filterWay)
    .via(denormalizeWayFlow())
}
