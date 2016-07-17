package io.plasmap.geo.producing

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, SinkShape}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.util.KafkaTopics._
import io.plasmap.geo.util.KafkaUtil._
import io.plasmap.model.{OsmNode, OsmObject, OsmRelation, OsmWay}
import io.plasmap.parser.OsmParser
import io.plasmap.serializer.OsmSerializer._
import org.slf4j.LoggerFactory

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


    val kafka = kafkaSink(kafkaHost)

    val source: Source[OsmObject, NotUsed] =
      parser(fileName)
        .collect { case Some(element) => element }
        .drop(offset)
        .map(countParsed)

    val nodeSink = Flow[OsmNode]
      .map(countProduced)
      .map(toBinary)
      .map(bytesToProducerRecord(nodesTopic))
      .to(kafka)

    val waySink = Flow[OsmWay]
      .map(countProduced)
      .map(toBinary)
      .map(bytesToProducerRecord(waysTopic))
      .to(kafka)

    val relationSink = Flow[OsmRelation]
      .map(countProduced)
      .map(toBinary)
      .map(bytesToProducerRecord(relationsTopic))
      .to(kafka)

    val branch = branchElements(nodeSink, waySink, relationSink)

    log.info(s"${Console.GREEN}Producing at kafka[$kafkaHost], zk[$zkHost]${Console.RESET}")
    source.to(branch).run()
    log.info(s"Running producer...")

  }

  def branchElements(nodeSink: Sink[OsmNode, NotUsed], waySink: Sink[OsmWay, NotUsed], relationSink: Sink[OsmRelation, NotUsed]): Sink[OsmObject, NotUsed] =
    Sink.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[OsmObject](3))

      broadcast.out(0) ~> Flow[OsmObject]
        .collect { case node: OsmNode => node } ~> nodeSink

      broadcast.out(1) ~> Flow[OsmObject]
        .collect { case way: OsmWay => way } ~> waySink

      broadcast.out(2) ~> Flow[OsmObject]
        .collect { case relation: OsmRelation => relation } ~> relationSink

      SinkShape(broadcast.in)
    })


  def displayUsage() = {
    println("Usage OsmProducer <fileName> [offset] [typ]")
  }

  def main(args: Array[String]): Unit = {
    args match {
      case Array(fileName, offset, typ) =>
        produce(fileName, offset.toInt, Some(typ))
      case Array(fileName, offset) =>
        produce(fileName, offset.toInt)
      case Array(fileName) =>
        produce(fileName, 0)
      case _ => displayUsage()
    }
    waitForUserInput()

  }

  def waitForUserInput(): Unit = {
    log.info(s"${Console.RED}Press key to exit.${Console.RESET}")
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
