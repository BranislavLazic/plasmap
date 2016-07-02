package io.plasmap.geo.preprocessing

import akka.NotUsed
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source, SubFlow}
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

object WayFlow{

  /**
    * Creates the denormalization flow for ways.
    *
    * @param mapNd          Function to retrieve node to hash mapping
    * @param denormalizeWay Function to denormalize a way
    * @return The flow
    */
  def denormalizeWayFlow(
                          mapNd: (OsmId) => Future[Option[OsmNodeMapping]],
                          denormalizeWay: (OsmWay, Map[OsmId, Point]) => OsmDenormalizedWay = Denormalizer.denormalizeWay
                        )(implicit ec:ExecutionContext,mat:Materializer):
  Flow[OsmWay, FlowError \/ OsmDenormalizedWay, NotUsed] =

    Flow[OsmWay].mapAsync(4)(way =>
      Source
        .single(way)
        .mapConcat(_.nds)
        .mapAsync(4)(mapNd)
        .filter(_.isDefined)
        .map(_.get)
        .map(mapping => mapping.osmId -> Point(mapping.hash))
        .runFold(Map.empty[OsmId, Point])((acc, mapping) => acc + mapping)
        .map(nds => denormalise(way, nds))
    )

  def denormalise(way: OsmWay, mappings: Map[OsmId, Point],
                  denormalizeWay: (OsmWay, Map[OsmId, Point]) => OsmDenormalizedWay = Denormalizer.denormalizeWay): \/[CouldNotDenormaliseObject, OsmDenormalizedWay] = {
    \/.fromTryCatchNonFatal(denormalizeWay(way, mappings))
      .leftMap(t => CouldNotDenormaliseObject(way, t))
  }
}
