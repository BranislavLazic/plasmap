package io.plasmap.geo.preprocessing

import java.io.IOException

import akka.stream.ActorAttributes._
import akka.stream.Supervision._
import akka.stream.scaladsl.Flow
import io.plasmap.geo.data.{OsmStorageService, OsmBB}
import io.plasmap.model.{OsmId, OsmDenormalizedObject}

import scala.concurrent.{Future, ExecutionContext}
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
  Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = {
    val subFlow: Flow[OsmDenormalizedObject, (OsmId, Option[OsmBB]), Unit] = Flow[OsmDenormalizedObject]
      .map(toBB)
      .log(s"BoundingBoxCreated")
      .mapAsync(16)((data) => {
        storeOsmBB(data)
          .map(toTuple(data.osmId))(ec)
      })
      .withAttributes(supervisionStrategy(resumingDecider))
      .log("PersistData")

    import scalaz.{Sink => _, Source => _, _}

    val validatedFlow: Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = subFlow
      .log("PersistDataGrouped")
      .map {
        case (osmId, Some(data)) =>
          \/-(osmId)

        case (osmId, None) =>
          -\/(DataPersisterError(new IOException(s"Failed to persist data for $osmId")))
      }
    validatedFlow
  }

  def toTuple(osmId: OsmId): (Option[OsmBB]) => (OsmId, Option[OsmBB]) = {
    x => {
      osmId -> x
    }
  }
}
