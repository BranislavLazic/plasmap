package io.plasmap.util.streams

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, Zip}
import akka.stream.stage._
import com.softwaremill.react.kafka.ProducerProperties
import io.plasmap.model.{OsmDenormalizedObject, OsmObject}
import kafka.serializer.Encoder

import scala.concurrent.{ExecutionContext, Future}

final class InfiniteRepeat[In](extrapolate: In ⇒ Iterator[In]) extends GraphStage[FlowShape[In, In]] {

  private val in = Inlet[In]("expand.in")
  private val out = Outlet[In]("expand.out")

  override def initialAttributes = Attributes.none
  override val shape = FlowShape(in, out)

  override def createLogic(attr: Attributes) = new GraphStageLogic(shape) {
    private var iterator: Iterator[In] = Iterator.empty
    private var expanded = false

    override def preStart(): Unit = pull(in)

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        iterator = extrapolate(grab(in))
        if (iterator.hasNext) {
          if (isAvailable(out)) {
            expanded = true
            pull(in)
            push(out, iterator.next())
          } else expanded = false
        } else pull(in)
      }
      override def onUpstreamFinish(): Unit = {

      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (iterator.hasNext) {
          if (!expanded) {
            expanded = true
            if (isClosed(in)) {
              push(out, iterator.next())
              completeStage()
            } else {
              // expand needs to pull first to be “fair” when upstream is not actually slow
              pull(in)
              push(out, iterator.next())
            }
          } else push(out, iterator.next())
        }
      }
    })
  }
}

object InfiniteRepeat {

  def apply[In]:InfiniteRepeat[In] = new InfiniteRepeat(Iterator.continually[In](_))

}

/**
  * @author Jan Schulte <jan@plasmap.io>
  */
object Utilities {


  object OsmObjectEncoder extends Encoder[OsmObject] {

    import io.plasmap.serializer.OsmSerializer._

    override def toBytes(t: OsmObject): Array[Byte] = {
      toBinary(t)
    }

    def partitionizer(osmObject: OsmObject) =
      Some(osmObject.id.value.toString.getBytes("UTF8"))
  }

  object OsmDenormalisedObjectEncoder extends Encoder[OsmDenormalizedObject] {

    import io.plasmap.serializer.OsmDenormalizedSerializer._

    override def toBytes(t: OsmDenormalizedObject): Array[Byte] = {
      toBinary(t)
    }

    def partitionizer(osmDenormalizedObject: OsmDenormalizedObject) =
      Some(osmDenormalizedObject.id.value.toString.getBytes("UTF8"))
  }


  def createSinkProperties[T](host: String, topic: String, clientId: String, encoder: Encoder[T], partitionizer: (T) => Option[Array[Byte]]): ProducerProperties[T] =
    ProducerProperties(host, topic, clientId, encoder, partitionizer)
      .asynchronous()
      .noCompression()



  def mapConcatAndGroup[I, O, M](source: Source[I, M], mapConcatF: (I) => List[O]): Source[(I, O), M] = {
    val mappedSource: Source[(I, O), M] = source.mapConcat(
      (input) => mapConcatF(input).map(input -> _)
    )
    mappedSource
  }

  def mapConcatAndGroupAsync[I, O, M](source: Source[I, M], mapConcatF: (I) => Future[List[O]])(implicit ec: ExecutionContext): Source[(I, O), M] = {
    val mappedSource: Source[List[(I, O)], M] = source
      .mapAsync(4)((input) =>
        mapConcatF(input)
          .map((list) => list.map((output) => input -> output)))

    mappedSource.mapConcat(identity _)
  }

  def reduceByKey[In, K, Out](
                               maximumGroupSize: Int,
                               groupKey: (In) => K,
                               map: (In) => Out)(reduce: (Out, Out) => Out): Flow[In, (K, Out), NotUsed] = {
    Flow[In]
      .groupBy[K](maximumGroupSize, groupKey)
      .map(e => groupKey(e) -> map(e))
      .reduce((l, r) => l._1 -> reduce(l._2, r._2))
      .mergeSubstreams
  }

  def groupAndMapSubFlow[In, K, Out](groupKey: (In) => K,
                                     subFlow: Flow[In, Out, NotUsed],
                                     maximumGroupSize: Int
                               ): Flow[In, Out, NotUsed] = {

    Flow[In]
      .groupBy[K](maximumGroupSize, groupKey)
      //.map(e => groupKey(e) -> e)
      .via(subFlow)
      //.reduce((l, r) => l._1 -> reduce(l._2, r._2))
      .mergeSubstreams
  }


  def subflowWithGroupKey[In, K, Out](subFlow: Flow[In, Out, NotUsed], groupKey: (In) => K): Flow[In, (K, Out), NotUsed] = Flow.fromGraph {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[In](outputPorts = 2, eagerCancel = true))
      val repeater = b.add(InfiniteRepeat[K])
      val zip = b.add(Zip[K, Out])

      broadcast ~>                            subFlow ~> zip.in1
      broadcast ~> Flow[In].map(groupKey) ~> repeater ~> zip.in0

      FlowShape(broadcast.in, zip.out)
    }
  }

  def groupAndMapSubflowWithKey[In, K, Out](
                                groupKey: (In) => K,
                                subFlow: Flow[In, Out, NotUsed],
                                maximumGroupSize: Int): Flow[In, (K, Out), NotUsed] = {

    val subFlowWithGroupKey: Flow[In, (K, Out), NotUsed] = Utilities.subflowWithGroupKey(subFlow, groupKey)

    groupAndMapSubFlow[In,K,(K,Out)](groupKey,subFlowWithGroupKey,100)
  }

  /*def groupAndMap[In, K, Out](
                               maximumGroupSize: Int,
                               groupKey: (In) => K,
                               flow: Flow[In, Out, NotUsed],
                               strategy: OverflowStrategy = OverflowStrategy.fail
                             )(implicit mat: Materializer, ec: ExecutionContext): Flow[In, (K, List[Out]), NotUsed] = {

    def foldF(accumulator: List[Out], elem: Out): List[Out] = elem :: accumulator

    val zero = List.empty[Out]

    val foldSubstream: (K, Source[In, Unit]) => Future[(K, List[Out])] = (key, subStream) =>
      subStream
        .via(flow)
        .runFold(zero)(foldF)
        .map(key -> _)

    val groupFlow: SubFlow[In, NotUsed, Flow[In, _, NotUsed], Sink[In, NotUsed]] = Flow[In]
      .groupBy(1000, groupKey)


    val via: SubFlow[_, NotUsed, Flow[In, Out, NotUsed], Sink[In, NotUsed]][Out] = groupFlow.via(flow)
    via.merg

      .map(foldSubstream.tupled)
      .buffer(maximumGroupSize, strategy)
      .mapAsync(4)(identity _)
  }*/

  // Flatten a materialized stream of streams
  /*def flatten[K, Out](source: Source[(K, List[Out]), Unit])(implicit mat: Materializer): Source[(K, Out), Unit] = {
    source.mapConcat((input) => {
      val (k, list) = input
      list.map(k -> _)
    })
  }*/

}
