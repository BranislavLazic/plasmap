package io.plasmap.geo.preprocessing

import java.io.IOException

import akka.stream.ActorAttributes._
import akka.stream.Supervision._
import akka.stream.scaladsl.Flow
import io.plasmap.geo.data.{OsmStorageService, OsmBBTag}
import io.plasmap.model.{OsmId, OsmDenormalizedObject}

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
 * Created by janschulte on 01/03/16.
 */
case class DataByTagPersister(ec:ExecutionContext) {

  /**
   * COLD START
   */
  lazy val storageService = OsmStorageService()

  private def defaultStoreOsmBBTag(bbTag: OsmBBTag): Future[Option[OsmBBTag]] = storageService.insertBBTag(bbTag)(ec)

  def createPersistDataByTagFlow(
                                  toBBTag: (OsmDenormalizedObject) => List[OsmBBTag] = ProcessingUtilities.toBBTag,
                                  storeOsmBBTag: (OsmBBTag) => Future[Option[OsmBBTag]] = defaultStoreOsmBBTag
                                  ): Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = {

    val subFlow: Flow[OsmDenormalizedObject, (OsmId, Option[OsmBBTag]), Unit] = Flow[OsmDenormalizedObject]
      .mapConcat(toBBTag)
      .log(s"BoundingBoxTagsCreated")
      .mapAsync(16)((data) => storeOsmBBTag(data).map(x => {
        data.osmId -> x
      })(ec))
      .withAttributes(supervisionStrategy(resumingDecider))
      .log("PersistDataTag")

    import scalaz.{Sink => _, Source => _, _}

    val validatedFlow: Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = subFlow
      .log("PersistDataTagGrouped")
      .map {
        case (osmId, Some(data)) =>
          \/-(osmId)

        case (osmId, None) =>
          -\/(DataByTagPersisterError(new IOException(s"Failed to persist data by tag for $osmId")))
      }
    validatedFlow
  }
}
