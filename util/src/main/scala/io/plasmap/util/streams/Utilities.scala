package io.plasmap.util.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.softwaremill.react.kafka.{ProducerProperties, ReactiveKafka}
import io.plasmap.model.{OsmDenormalizedObject, OsmObject}
import kafka.serializer.Encoder

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
object Utilities {


  object OsmObjectEncoder extends Encoder[OsmObject]{
    import io.plasmap.serializer.OsmSerializer._
    override def toBytes(t: OsmObject): Array[Byte] = {
      toBinary(t)
    }

    def partitionizer(osmObject: OsmObject) =
      Some(osmObject.id.value.toString.getBytes("UTF8"))
  }

  object OsmDenormalisedObjectEncoder extends Encoder[OsmDenormalizedObject]{
    import io.plasmap.serializer.OsmDenormalizedSerializer._
    override def toBytes(t: OsmDenormalizedObject): Array[Byte] = {
      toBinary(t)
    }
    def partitionizer(osmDenormalizedObject: OsmDenormalizedObject) =
      Some(osmDenormalizedObject.id.value.toString.getBytes("UTF8"))
  }


  def createSinkProperties[T](host: String, topic: String, clientId:String, encoder:Encoder[T],partitionizer:(T)=> Option[Array[Byte]]): ProducerProperties[T] =
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

    mappedSource.mapConcat(identity)
  }

  def reduceByKey[In, K, Out](
                               maximumGroupSize: Int,
                               groupKey: (In) => K,
                               foldZero: (K) => Out,
                               fold: (Out, In) => Out
                               )(implicit mat: Materializer): Flow[In, (K, Out), Unit] = {

    val groupStreams = Flow[In].groupBy(groupKey)
    val reducedValues = groupStreams.map {
      case (key, groupStream) =>
        groupStream.runFold((key, foldZero(key))) {
          case ((k, aggregated), elem) => (k, fold(aggregated, elem))
        }
    }

    val buffer: Flow[In, Future[(K, Out)], Unit] = reducedValues.buffer(maximumGroupSize, OverflowStrategy.fail)
    buffer.mapAsync(4)(identity)
  }

  def groupAndMap[In, K, Out](
                               maximumGroupSize: Int,
                               groupKey: (In) => K,
                               flow: Flow[In, Out, Unit],
                               strategy: OverflowStrategy = OverflowStrategy.fail
                               )(implicit mat: Materializer, ec: ExecutionContext): Flow[In, (K, List[Out]), Unit] = {

    val groupStreams: Flow[In, (K, Source[In, Unit]), Unit] = Flow[In].groupBy(groupKey)
    val reducedValues: Flow[In, Future[(K, List[Out])], Unit] = groupStreams.map {
      case (key: K, groupStream: Source[In, Unit]) =>
        val mappedSource: Source[Out, Unit] = groupStream.via(flow)
        mappedSource.runFold((key, List.empty[Out])) {
          case ((key: K, aggregated: List[Out]), elem: Out) =>
            (key, elem :: aggregated)
        }
    }

    val buffer: Flow[In, Future[(K, List[Out])], Unit] = reducedValues.buffer(maximumGroupSize, strategy)
    buffer.mapAsync(4)(identity)
  }

  // Flatten a materialized stream of streams
  def flatten[K, Out](source: Source[(K, List[Out]), Unit])(implicit mat: Materializer): Source[(K, Out), Unit] = {
    source.mapConcat((input) => {
      val (k, list) = input
      list.map(k -> _)
    })
  }


}
