package io.plasmap.geo.preprocessing

import java.io.IOException

import akka.stream.ActorAttributes._
import akka.stream.Supervision._
import akka.stream.scaladsl.Flow
import io.plasmap.geo.mappings.{MappingService, OsmMapping}
import io.plasmap.model.{OsmDenormalizedObject, OsmId}

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
 * Created by janschulte on 01/03/16.
 */
case class MappingPersister(ec:ExecutionContext) {

  lazy val mappingService = MappingService()(ec)

  private def defaultStoreMapping(m: OsmMapping): Future[Option[OsmMapping]] = mappingService.insertMapping(m)(ec)

  def createPersistMappingFlow(toMapping: (OsmDenormalizedObject) => OsmMapping = ProcessingUtilities.toMapping,
                               storeMapping: (OsmMapping) => Future[Option[OsmMapping]] = defaultStoreMapping
                                ): Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = {

    import scalaz.{Sink => _, Source => _, _}

    val subFlow: Flow[OsmDenormalizedObject, (OsmId, Option[OsmMapping]), Unit] = Flow[OsmDenormalizedObject]
      .map(toMapping)
      .log(s"MappingCreated")
      .mapAsync(16)((mapping) => storeMapping(mapping).map(x => {
        mapping.osmId -> x
      })(ec))
      .withAttributes(supervisionStrategy(resumingDecider))
      .log("PersistMapping")


    val validatedFlow: Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = subFlow
      .log("PersistMappingGrouped")
      .map {
        case (osmId, Some(mapping)) =>
          \/-(osmId)
        case (osmId, None) =>
          -\/(MappingPersisterError(new IOException(s"Failed to store mapping $osmId")))
      }

    validatedFlow
  }
}
