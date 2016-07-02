package io.plasmap.geo.producing

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import com.softwaremill.react.kafka.{ProducerProperties, ReactiveKafka}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.model.OsmObject
import io.plasmap.parser.OsmParser
import io.plasmap.util.GeowUtils._
import io.plasmap.util.streams.Utilities
import io.plasmap.util.streams.Utilities.OsmObjectEncoder
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Deadline
import scala.io.{Codec, StdIn}

/**
 * Main class for producing osm elements to Kafka MQ.
  *
  * @author Jan Schulte <jan@plasmap.io>
 */
object OsmProducer {

  lazy val log = Logger(LoggerFactory.getLogger(OsmProducer.getClass.getName))

  lazy implicit val actorSystem = ActorSystem("producer")
  lazy implicit val materializer = ActorMaterializer()

  lazy val config = ConfigFactory.load()
  lazy val kafkaHost = config.getString("plasmap.producing.kafka")
  lazy val zkHost = config.getString("plasmap.producing.zk")

  lazy val kafka = new ReactiveKafka()

  var producedMessagesCount = 0L
  var parsedMessagesCount = 0L

  def countParsed[T](parsed: T): T = {
    if (parsedMessagesCount % 100000 == 0) {
      log.info(s"Parsed $parsedMessagesCount elements in total.")
    }
    parsedMessagesCount += 1
    parsed
  }

  var lastTimestamp = Deadline.now
  var lastProducedMessagesCount = 0L

  def countProduced[T](encoded: T): T = {

    val timestamp = Deadline.now
    val timeframe: Long = (timestamp - lastTimestamp) toSeconds

    if (timeframe >= 1) {
      lastTimestamp = timestamp
      val messageDifference = producedMessagesCount - lastProducedMessagesCount
      val messageRate = messageDifference / timeframe
      log.info(s"Produced $producedMessagesCount elements in total at a rate of: $messageDifference / $timeframe s (=$messageRate/s)")

      lastProducedMessagesCount = producedMessagesCount
    }

    producedMessagesCount += 1
    encoded
  }

  var messageSize = 0L

  def countSize(encoded: Array[Byte]): Array[Byte] = {

    val currentSize = encoded.length
    messageSize += currentSize

    if (producedMessagesCount % 100000 == 0) {

      val messageSizeDifference = if (producedMessagesCount == 0) 0L else messageSize / producedMessagesCount
      log.info(s"Message size at $messageSizeDifference bytes / element")

      lastProducedMessagesCount = producedMessagesCount
    }

    encoded
  }

  def produce(fileName: String, offset: Int = 0, typ: Option[String] = None) = {

    val nodeSinkProperties: ProducerProperties[OsmObject] = Utilities.createSinkProperties(kafkaHost, "osm_nodes", "preprocessing", OsmObjectEncoder, OsmObjectEncoder.partitionizer)
    val nodeSink: Sink[OsmObject, NotUsed] = Sink.fromSubscriber(kafka.publish(nodeSinkProperties))

    val waySinkProperties: ProducerProperties[OsmObject] = Utilities.createSinkProperties(kafkaHost, "osm_ways", "preprocessing", OsmObjectEncoder, OsmObjectEncoder.partitionizer)
    val waySink: Sink[OsmObject, NotUsed] =  Sink.fromSubscriber(kafka.publish(waySinkProperties))

    val relationSinkProperties: ProducerProperties[OsmObject] = Utilities.createSinkProperties(kafkaHost, "osm_relations", "preprocessing", OsmObjectEncoder, OsmObjectEncoder.partitionizer)
    val relationSink: Sink[OsmObject, NotUsed] = Sink.fromSubscriber(kafka.publish(relationSinkProperties))

    val source: Source[OsmObject, NotUsed] =
      parser(fileName)
        .filter(_.isDefined)
        .drop(offset)
        .map(countParsed)
        .map(_.get)

    val flow: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val input = builder.add(source)
      val bcast = builder.add(Broadcast[OsmObject](3))

      val nodesOutput = builder.add(nodeSink)
      val waysOutput = builder.add(waySink)
      val relationsOutput = builder.add(relationSink)

      input ~> bcast.in
      bcast.out(0) ~> Flow[OsmObject]
        .filter(isNode)
        .filter( _ => typ.isEmpty || typ.contains("node"))
        .map(countProduced) ~> nodesOutput
      bcast.out(1) ~> Flow[OsmObject]
        .filter(isWay)
        .filter( _ => typ.isEmpty || typ.contains("way"))
        .map(countProduced) ~> waysOutput
      bcast.out(2) ~> Flow[OsmObject]
        .filter(isRelation)
        .filter( _ => typ.isEmpty || typ.contains("relation"))
        .map(countProduced) ~> relationsOutput

      ClosedShape
    })

    log.info(s"${Console.GREEN}Producing at kafka[${kafkaHost}], zk[$zkHost]${Console.RESET}")
    flow.run()
    println(s"Running producer...")

  }

  def displayUsage() = {
    println("Usage OsmProducer <fileName> [offset] [typ]")
  }

  def main(args: Array[String]): Unit = {
    args match {
      case Array(fileName, offset, typ) =>
        produce(fileName,offset.toInt, Some(typ))
      case Array(fileName, offset) =>
        produce(fileName,offset.toInt)
      case Array(fileName) =>
        produce(fileName,0)
      case _ => displayUsage()
    }
    waitForUserInput()

  }

  def waitForUserInput(): Unit = {
    println(s"${Console.RED}Press key to exit.${Console.RESET}")
    StdIn.readLine()
    import scala.concurrent.ExecutionContext.Implicits.global
    val f = actorSystem.terminate()
    f.onComplete {
      case _ ⇒ System.exit(0)
    }

  }

  def parser(fileName: String) = {
    val parser = () => OsmParser(fileName)(Codec.UTF8)
    Source.fromIterator(parser)
  }


}
