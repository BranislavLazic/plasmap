package io.plasmap.geo.preprocessing

import java.io.IOException

import akka.NotUsed
import akka.stream.ActorAttributes._
import akka.stream.Supervision._
import akka.stream.scaladsl.Flow
import io.plasmap.geo.data.{OsmBB, OsmStorageService}
import io.plasmap.model.{OsmDenormalizedObject, OsmId}

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
 * Created by janschulte on 01/03/16.
 */
case class DataPersister(ec:ExecutionContext) {

  lazy val storageService = OsmStorageService()

  private def defaultStoreOsmBB(bb: OsmBB): Future[Option[OsmBB]] = storageService.insertBB(bb)(ec)

  def createPersistDataFlow(
                             toBB: (OsmDenormalizedObject) => OsmBB = ProcessingUtilities.toBB,
                             storeOsmBB: (OsmBB) => Future[Option[OsmBB]] = defaultStoreOsmBB):
  Flow[OsmDenormalizedObject, FlowError \/ OsmId, NotUsed] = {
    val subFlow: Flow[OsmDenormalizedObject, (OsmId, Option[OsmBB]), NotUsed] = Flow[OsmDenormalizedObject]
      .map(toBB)
      .log(s"BoundingBoxCreated")
      .mapAsync(16)((data) => {
        storeOsmBB(data)
          .map(tuplify(data.osmId))(ec)
      })
      .withAttributes(supervisionStrategy(resumingDecider))
      .log("PersistData")

    import scalaz.{Sink => _, Source => _, _}

    val validatedFlow: Flow[OsmDenormalizedObject, FlowError \/ OsmId, NotUsed] = subFlow
      .log("PersistDataGrouped")
      .map {
        case (osmId, Some(data)) =>
          \/-(osmId)

        case (osmId, None) =>
          -\/(DataPersisterError(new IOException(s"Failed to persist data for $osmId")))
      }
    validatedFlow
  }

  def tuplify(osmId: OsmId): (Option[OsmBB]) => (OsmId, Option[OsmBB]) = osmId -> _

}
