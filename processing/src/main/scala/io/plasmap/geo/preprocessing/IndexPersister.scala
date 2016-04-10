package io.plasmap.geo.preprocessing

import akka.actor.ActorRef
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import io.plasmap.geo.mappings.{IndexMapping, IndexingService}
import io.plasmap.model.{OsmDenormalizedObject, OsmId}
import org.reactivestreams.Publisher

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
 * Created by janschulte on 29/02/16.
 */
case class IndexPersister(ec:ExecutionContext, mat:Materializer) {

  lazy val indexingService = IndexingService()

  type CH = (OsmId) => Unit
  type EH = (Throwable) => Unit

  private def defaultCreateIndexSink(ch:CH, eh:EH):Sink[IndexMapping,Unit] = indexingService.indexOsmObjectSink(successFn = ch, errorHandler = eh)

  def createPersistIndexFlow(
                              toIndex: (OsmDenormalizedObject) => Option[IndexMapping] = ProcessingUtilities.toIndex,
                              createIndexSink: (CH, EH) => Sink[IndexMapping,Unit] = defaultCreateIndexSink
                              ): Flow[OsmDenormalizedObject, FlowError \/ OsmId, Unit] = {

    import scalaz.{Sink => _, Source => _, _}

    val (actorRef: ActorRef, publisher: Publisher[FlowError \/ OsmId]) = Source.actorRef[FlowError \/ OsmId](1000, OverflowStrategy.dropHead).toMat(Sink.publisher)(Keep.both).run()(mat)

    def ch(osmId:OsmId):Unit = {
      actorRef ! \/-(osmId)
    }

    def eh(t:Throwable):Unit = {
      actorRef ! -\/(IndexPersisterError(t))
    }

    val sink: Sink[IndexMapping, Unit] = createIndexSink(ch,eh)

    val inputSink: Sink[OsmDenormalizedObject, Unit] = Flow[OsmDenormalizedObject]
      .map(toIndex)
      .filter(_.isDefined)
      .map(_.get)
      .log(s"IndexTagsCreated")
      .to(sink)

    val outputSource: Source[FlowError \/ OsmId, Unit] = Source[FlowError \/ OsmId](publisher)

    Flow.wrap(inputSink,outputSource)( (u1:Unit, u2:Unit) => Unit)
  }
}
