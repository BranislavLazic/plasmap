package io.plasmap.geo.preprocessing

import _root_.io.plasmap.geo.data.{OsmBB, OsmStorageService}
import _root_.io.plasmap.model._
import _root_.io.plasmap.serializer.{OsmDenormalizedSerializer, OsmSerializer}
import _root_.io.plasmap.util.streams.Utilities.{OsmDenormalisedObjectEncoder, OsmObjectEncoder}
import akka.NotUsed
import akka.actor._
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision.resumingDecider
import akka.stream._
import akka.stream.scaladsl.{Sink, Source, _}
import com.softwaremill.react.kafka.{ConsumerProperties, ProducerProperties, ReactiveKafka}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings.{MappingService, OsmMapping, OsmNodeMapping}
import kafka.serializer.DefaultDecoder
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.Try
import scalaz._

/**
  * Main class for preprocessing osm objects.
  *
  * @author Jan Schulte
  */
object OsmPreprocessor {

  val log = Logger(LoggerFactory.getLogger(OsmPreprocessor.getClass.getName))

  implicit lazy val actorSystem = ActorSystem("preprocessing")
  implicit lazy val materializer = ActorMaterializer()

  lazy val mappingEC = actorSystem.dispatchers.lookup("plasmap.preprocessing.mapping-dispatcher")
  lazy val denormalisingEC = actorSystem.dispatchers.lookup("plasmap.preprocessing.denormalising-dispatcher")
  lazy val indexingEC = actorSystem.dispatchers.lookup("plasmap.preprocessing.indexing-dispatcher")
  lazy val dataEC = actorSystem.dispatchers.lookup("plasmap.preprocessing.data-dispatcher")
  lazy val dataTagEC = actorSystem.dispatchers.lookup("plasmap.preprocessing.data-tag-dispatcher")

  /**
    * COLD START
    */
  lazy val storageService = OsmStorageService()
  lazy val mappingService = MappingService()(mappingEC)

  lazy val config = ConfigFactory.load()
  lazy val kafkaHost = config.getString("plasmap.preprocessing.kafka")
  lazy val zkHost = config.getString("plasmap.preprocessing.zk")

  lazy val kafka = new ReactiveKafka()

  var processedElementCounter = 0L
  var parsedElementCounter = 0L

  def parsedCounter[T <: OsmObject](typ: OsmType)(parsed: T): T = {
    val rate: Int = typ match {
      case OsmTypeNode => 100000
      case OsmTypeWay => 10000
      case OsmTypeRelation => 100
    }
    if (parsedElementCounter % rate == 0) {
      log.debug(s"Parsed $parsedElementCounter elements in total. Currently at ${parsed.id}.")
    }
    parsedElementCounter += 1
    parsed
  }

  def processedCounter[T](typ: OsmType)(elem: T): T = {
    val rate: Int = typ match {
      case OsmTypeNode => 1000000
      case OsmTypeWay => 100000
      case OsmTypeRelation => 1000
    }
    if (processedElementCounter % rate == 0) {
      log.info(s"Consumed $processedElementCounter elements in total.")
    }
    processedElementCounter += 1
    elem
  }

  val nodesTopic: String = "osm_nodes"
  val nodesFailedTopic: String = "osm_nodes_failed"
  val waysTopic: String = "osm_ways"
  val waysFailedTopic: String = "osm_ways_failed"
  val relationsTopic: String = "osm_relations"
  val relationsFailedTopic: String = "osm_relations_failed"

  val clientId = "preprocessing"
  val persisterTopic = "osm_denormalised"
  val persisterIndexGroup: String = "index"
  val persisterDataGroup: String = "data"
  val persisterDataByTagGroup: String = "data_tag"
  val persisterMappingGroup: String = "mapping"

  def createErrorFlow(failureTopic: String) = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._
      val branch = builder.add(Broadcast[FlowError \/ OsmDenormalizedObject](2))

      val rightOutput = builder.add(Flow[OsmDenormalizedObject])

      val failureProperties: ProducerProperties[OsmObject] =
        ProducerProperties(kafkaHost, failureTopic, clientId, OsmObjectEncoder, OsmObjectEncoder.partitionizer)
          .asynchronous()
          .noCompression()
      val failureSink = Sink.fromSubscriber(kafka.publish(failureProperties))

      val logging: Flow[FlowError, OsmObject, NotUsed] = Flow[FlowError]
        .filter(_.isInstanceOf[CouldNotDenormaliseObject])
        .map(_.asInstanceOf[CouldNotDenormaliseObject])
        .map((cndw) => {
          log.error(s"Failed to denormalise relation ${cndw.osmObject.id}: ${cndw.toString}")
          cndw.osmObject
        })

      // Successfully denormalised objects are passed on to the next step
      val filterSuccessful: Flow[Disjunction[FlowError, OsmDenormalizedObject], OsmDenormalizedObject, NotUsed] = Flow[FlowError \/ OsmDenormalizedObject]
        .filter(_.isRight)
        .map(_.toEither.right.get)

      branch.out(0) ~> filterSuccessful ~> rightOutput

      // Failed denormalised objects get logged and send to error queue
      val filterFailed: Flow[Disjunction[FlowError, OsmDenormalizedObject], FlowError, NotUsed] = Flow[FlowError \/ OsmDenormalizedObject]
        .filter(_.isLeft)
        .map(_.toEither.left.get)

      branch.out(1) ~> filterFailed ~> logging ~> failureSink

      FlowShape(branch.in, rightOutput.outlet)
    })

  def denormalise(typ: OsmType, failed: Boolean = false, offset: Int = 0) = {

    val sourceTopic = getTopic(typ, failed)
    val failureTopic = getTopic(typ, failed = true)

    val props = createConsumerProps(zkHost, kafkaHost, sourceTopic, "preprocessing")

    val source: Source[Array[Byte], NotUsed] = Source.fromPublisher(kafka.consume(props))
      .withAttributes(supervisionStrategy(resumingDecider)) // Prevent kafka error to fail whole pipeline
      .drop(offset)
      .map(_.message()) // Extract content

    log.info(s"${Console.GREEN}Kafka consumer initializer with:\n${props.dump}${Console.RESET}")

    val successProperties: ProducerProperties[OsmDenormalizedObject] = ProducerProperties(kafkaHost, persisterTopic, clientId, OsmDenormalisedObjectEncoder, OsmDenormalisedObjectEncoder.partitionizer)
      .asynchronous()
      .noCompression()
    val successSink = Sink.fromSubscriber(kafka.publish(successProperties))

    val denormalisationFlow = Flow[Array[Byte]]
      .map(OsmSerializer.fromBinary) // Deserialise
      .map(logFailure)
      .filter(_.isSuccess) // Filter out the stuff that could not be deserialised
      .map(_.get) // Actually get the deserialised values
      .map(parsedCounter(typ)) // Count how many nodes were parsed
      .via(objectFlow(typ))
      .via(createErrorFlow(failureTopic))
      .map(processedCounter(typ))

    println(s"${Console.GREEN}Press key to start processing...${Console.RESET}")
    StdIn.readLine()
    source.via(denormalisationFlow).to(successSink).run()
    println(s"Running denormalisation for topic $sourceTopic.")
    waitForUserInput()
  }

  def persistIndex(offset: Int = 0) = {

    val group: String = persisterIndexGroup
    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      IndexPersister(indexingEC, materializer).createPersistIndexFlow()

    runPersister(persisterFlow, group, offset)
  }

  def persistMapping(offset: Int = 0) = {

    val group: String = persisterMappingGroup
    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      MappingPersister(mappingEC).createPersistMappingFlow()

    runPersister(persisterFlow, group, offset)
  }

  def dataPersister(offset: Int = 0) = {

    val group: String = persisterDataGroup
    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      DataPersister(dataEC).createPersistDataFlow()

    runPersister(persisterFlow, group, offset)
  }

  def dataByTagPersister(offset: Int = 0) = {

    val group: String = persisterDataByTagGroup
    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      DataByTagPersister(dataTagEC).createPersistDataByTagFlow()

    runPersister(persisterFlow, group, offset)
  }

  def runPersister(persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed], group: String, offset: Int): Unit = {
    val props = createConsumerProps(zkHost, kafkaHost, persisterTopic, group)

    val source: Source[Array[Byte], NotUsed] = Source.fromPublisher(kafka.consume(props))
      .withAttributes(supervisionStrategy(resumingDecider)) // Prevent kafka error to fail whole pipeline
      .drop(offset)
      .map(_.message()) // Extract content

    log.info(s"${Console.GREEN}Kafka consumer initializer with:\n${props.dump}${Console.RESET}")

    val successProperties: ProducerProperties[OsmDenormalizedObject] = ProducerProperties(kafkaHost, persisterTopic, clientId, OsmDenormalisedObjectEncoder, OsmDenormalisedObjectEncoder.partitionizer)
      .asynchronous()
      .noCompression()

    //val successSink = Sink(kafka.publish(successProperties))
    // for the moment don't do error logging & fixing
    val sink = Sink.ignore

    val flow: Flow[Array[Byte], FlowError \/ OsmId, NotUsed] = Flow[Array[Byte]]
      .map(OsmDenormalizedSerializer.fromBinary) // Deserialise
      .map(logFailure)
      .filter(_.isSuccess) // Filter out the stuff that could not be deserialised
      .map(_.get) // Actually get the deserialised values
      .via(persisterFlow)

    println(s"${Console.GREEN}Press key to start $group...${Console.RESET}")
    StdIn.readLine()
    source.via(flow).to(sink).run()
    println(s"Running persister $group.")
    waitForUserInput()
  }

  def main(args: Array[String]): Unit = {

    //Start of code
    args match {
      case Array("denormalise", "node", "failed", offset) =>
        denormalise(OsmTypeNode, true, offset.toInt)
      case Array("denormalise", "node", "failed") =>
        denormalise(OsmTypeNode, true)
      case Array("denormalise", "node", offset) =>
        denormalise(OsmTypeNode, false, offset.toInt)
      case Array("denormalise", "node") =>
        denormalise(OsmTypeNode)

      case Array("denormalise", "way", "failed", offset) =>
        denormalise(OsmTypeWay, true, offset.toInt)
      case Array("denormalise", "way", "failed") =>
        denormalise(OsmTypeWay, true)
      case Array("denormalise", "way", offset) =>
        denormalise(OsmTypeWay, false, offset.toInt)
      case Array("denormalise", "way") =>
        denormalise(OsmTypeWay)

      case Array("denormalise", "relation", "failed", offset) =>
        denormalise(OsmTypeRelation, true, offset.toInt)
      case Array("denormalise", "relation", "failed") =>
        denormalise(OsmTypeRelation, true)
      case Array("denormalise", "relation", offset) =>
        denormalise(OsmTypeRelation, false, offset.toInt)
      case Array("denormalise", "relation") =>
        denormalise(OsmTypeRelation)

      case Array("persist", "index", offset) =>
        persistIndex(offset.toInt)
      case Array("persist", "index") =>
        persistIndex(0)
      case Array("persist", "mapping", offset) =>
        persistMapping(offset.toInt)
      case Array("persist", "mapping") =>
        persistMapping(0)
      case Array("persist", "data", offset) =>
        dataPersister(offset.toInt)
      case Array("persist", "data") =>
        dataPersister()
      case Array("persist", "data_tag", offset) =>
        dataByTagPersister(offset.toInt)
      case Array("persist", "data_tag") =>
        dataByTagPersister()

      case _ => println("Usage OsmPreprocessor <denormalise node|way|relation [failed] [offset]>|<persist index,data,mapping")
        waitForUserInput()
    }
  }


  def logFailure[T](intent: Try[T]): Try[T] = {
    intent match {
      case scala.util.Success(_) =>
      case scala.util.Failure(e) => log.error(s"Failed to deserialise $e")
    }
    intent
  }

  def getTopic(typ: OsmType, failed: Boolean): String =
    if (failed) {
      getFailureTopic(typ)
    } else {
      getTopic(typ)
    }

  def getTopic(typ: OsmType): String = typ match {
    case OsmTypeNode => nodesTopic
    case OsmTypeWay => waysTopic
    case OsmTypeRelation => relationsTopic
  }

  def getFailureTopic(typ: OsmType): String = typ match {
    case OsmTypeNode => nodesFailedTopic
    case OsmTypeWay => waysFailedTopic
    case OsmTypeRelation => relationsFailedTopic
  }

  def createConsumerProps(zkHost: String, kafkaHost: String, topicName: String, groupId: String): ConsumerProperties[Array[Byte]] = {
    ConsumerProperties(
      brokerList = kafkaHost,
      zooKeeperHost = zkHost,
      topic = topicName,
      groupId = groupId,
      decoder = new DefaultDecoder())
      .setProperty("rebalance.max.retries", "10")
      .setProperty("rebalance.backoff.ms", "300000")
      .setProperty("fetch.message.max.bytes", "10000000")
      .setProperty("offsets.storage", "kafka")
      .setProperty("dual.commit.enabled", "true")
      .setProperty("zookeeper.session.timeout.ms", "300000")
      .consumerTimeoutMs(10000)
  }

  def objectFlow(typ: OsmType): Flow[OsmObject, FlowError \/ OsmDenormalizedObject, NotUsed] = typ match {
    case OsmTypeNode =>
      val denormalisationFlow = NodeFlow.denormaliseNodeFlow
      UtilityFlows.filterNode.via(denormalisationFlow)
    case OsmTypeWay =>
      val mapNd: (OsmId) => Future[Option[OsmNodeMapping]] = mappingService.findNodeMapping(_)(mappingEC)
      val denormalisationFlow = WayFlow.denormalizeWayFlow(mapNd)(denormalisingEC,materializer)
      UtilityFlows.filterWay.via(denormalisationFlow)
    case OsmTypeRelation =>
      val mapRef: (OsmId, OsmType) => Future[Option[OsmMapping]] = mappingService.findMapping(_, _)(mappingEC)
      val toData: (Long, OsmId, OsmType) => Future[Option[OsmBB]] = storageService.findBB(_, _, _)(dataEC)
      val denormalisationFlow = RelationFlow.denormalizeRelationFlow(mapRef, toData)(denormalisingEC,materializer)
      UtilityFlows.filterRelation.via(denormalisationFlow)
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

}
