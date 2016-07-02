package io.plasmap.geo.preprocessing

import akka.NotUsed
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
case class IndexPersister(ec: ExecutionContext, mat: Materializer) {

  lazy val indexingService = IndexingService()

  type CH = (OsmId) => Unit
  type EH = (Throwable) => Unit

  private def defaultCreateIndexSink(ch: CH, eh: EH): Sink[IndexMapping, NotUsed] = indexingService.indexOsmObjectSink(successFn = ch, errorHandler = eh)

  def createPersistIndexFlow(
                              toIndex: (OsmDenormalizedObject) => Option[IndexMapping] = ProcessingUtilities.toIndex,
                              createIndexSink: (CH, EH) => Sink[IndexMapping, NotUsed] = defaultCreateIndexSink
                            ): Flow[OsmDenormalizedObject, FlowError \/ OsmId, NotUsed] = {

    import scalaz.{Sink => _, Source => _, _}

    val (actorRef: ActorRef, publisher: Publisher[FlowError \/ OsmId]) =
      Source.actorRef[FlowError \/ OsmId](1000, OverflowStrategy.dropHead).toMat(Sink.asPublisher(true))(Keep.both).run()(mat)

    def ch(osmId: OsmId): Unit = {
      actorRef ! \/-(osmId)
    }

    def eh(t: Throwable): Unit = {
      actorRef ! -\/(IndexPersisterError(t))
    }

    val sink: Sink[IndexMapping, NotUsed] = createIndexSink(ch, eh)

    val inputSink: Sink[OsmDenormalizedObject, NotUsed] = Flow[OsmDenormalizedObject]
      .map(toIndex)
      .filter(_.isDefined)
      .map(_.get)
      .log(s"IndexTagsCreated")
      .to(sink)

    val outputSource: Source[FlowError \/ OsmId, NotUsed] = Source.fromPublisher[FlowError \/ OsmId](publisher)

    Flow.fromSinkAndSource(inputSink, outputSource)
  }
}
