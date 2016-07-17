package io.plasmap.geo.preprocessing

import _root_.io.plasmap.geo.data.{OsmBB, OsmStorageService}
import _root_.io.plasmap.model._
import _root_.io.plasmap.serializer.{OsmDenormalizedSerializer, OsmSerializer}
import akka.NotUsed
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Sink, _}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings.{MappingService, OsmMapping, OsmNodeMapping}
import io.plasmap.geo.util.KafkaTopics._
import io.plasmap.geo.util.LoggingUtil._
import io.plasmap.geo.util.OsmObjectEncoder.OsmObjectEncoder
import io.plasmap.geo.util.{KafkaUtil, OsmDenormalisedObjectEncoder}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.Success
import scalaz.{Failure => ZFailure, Success => ZSuccess, _}


/**
  * Main class for preprocessing osm objects.
  *
  * @author Jan Schulte
  */
object OsmPreprocessor {

  val log = Logger(LoggerFactory.getLogger("processing"))

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


  def createErrorFlow(failureTopic: String) = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._
      val branch = builder.add(Broadcast[FlowError \/ OsmDenormalizedObject](2))

      val rightOutput = builder.add(Flow[OsmDenormalizedObject])

      import KafkaUtil._
      val failureSink = Flow[Array[Byte]]
        .map(bytesToProducerRecord(failureTopic))
        .to(kafkaSink(kafkaHost))

      val encode = Flow[OsmObject]
        .map(OsmObjectEncoder.toBytes)

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

      branch.out(1) ~> filterFailed ~> logging ~> encode ~> failureSink

      FlowShape(branch.in, rightOutput.outlet)
    })


  def denormalise(typ: OsmType, failed: Boolean = false, offset: Int = 0) = {

    val sourceTopic = getTopic(typ, failed)
    val failureTopic = getTopic(typ, failed = true)

    import KafkaUtil._
    val source = kafkaSource(kafkaHost)(sourceTopic, consumerGroup)
      .map(consumerRecordToBytes)

    val successSink = Flow[OsmDenormalizedObject]
      .map(OsmDenormalisedObjectEncoder.toBytes)
      .map(bytesToProducerRecord(persisterTopic))
      .to(kafkaSink(kafkaHost))

    log.info(s"${Console.GREEN}Kafka consumer initialized.${Console.RESET}")

    val denormalisationFlow = Flow[Array[Byte]]
      .map(OsmSerializer.fromBinary) // Deserialise
      .map(logFailure[OsmObject](e => log.error(s"Failed to deserialise $e")))
      .collect { case Success(element) => element }
      .map(parsedCounter) // Count how many nodes were parsed
      .via(objectFlow(typ))
      .via(createErrorFlow(failureTopic))
      .map(processedCounter)

    log.info(s"${Console.GREEN}Press key to start processing...${Console.RESET}")
    StdIn.readLine()
    source.via(denormalisationFlow).to(successSink).run()
    log.info(s"Running denormalisation for topic $sourceTopic.")
    waitForUserInput()
  }

  def persistIndex(offset: Int = 0) = {

    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      IndexPersister(indexingEC, materializer).createPersistIndexFlow()

    runPersister(persisterFlow, persisterIndexGroup, offset)
  }

  def persistMapping(offset: Int = 0) = {

    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      MappingPersister(mappingEC).createPersistMappingFlow()

    runPersister(persisterFlow, persisterMappingGroup, offset)
  }

  def dataPersister(offset: Int = 0) = {

    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      DataPersister(dataEC).createPersistDataFlow()

    runPersister(persisterFlow, persisterDataGroup, offset)
  }

  def dataByTagPersister(offset: Int = 0) = {

    val persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed] =
      DataByTagPersister(dataTagEC).createPersistDataByTagFlow()

    runPersister(persisterFlow, persisterDataByTagGroup, offset)
  }

  def runPersister(persisterFlow: Flow[OsmDenormalizedObject, Disjunction[FlowError, OsmId], NotUsed], group: String, offset: Int): Unit = {

    import KafkaUtil._
    val source = kafkaSource(kafkaHost)(persisterTopic, group)
      .map(consumerRecordToBytes)

    log.info(s"${Console.GREEN}Kafka consumer initialised for topic $persisterTopic${Console.RESET}")

    // for the moment don't do error logging & fixing
    val sink = Sink.ignore

    val flow: Flow[Array[Byte], FlowError \/ OsmId, NotUsed] = Flow[Array[Byte]]
      .map(OsmDenormalizedSerializer.fromBinary) // Deserialise
      .map(logFailure(e => log.error(s"Failed to deserialise element: $e")))
      .collect { case Success(element) => element }
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


  def objectFlow(typ: OsmType): Flow[OsmObject, FlowError \/ OsmDenormalizedObject, NotUsed] = typ match {
    case OsmTypeNode =>
      val denormalisationFlow = NodeFlow.denormaliseNodeFlow
      UtilityFlows.filterNode.via(denormalisationFlow)
    case OsmTypeWay =>
      val mapNd: (OsmId) => Future[Option[OsmNodeMapping]] = mappingService.findNodeMapping(_)(mappingEC)
      val denormalisationFlow = WayFlow.denormalizeWayFlow(mapNd)(denormalisingEC, materializer)
      UtilityFlows.filterWay.via(denormalisationFlow)
    case OsmTypeRelation =>
      val mapRef: (OsmId, OsmType) => Future[Option[OsmMapping]] = mappingService.findMapping(_, _)(mappingEC)
      val toData: (Long, OsmId, OsmType) => Future[Option[OsmBB]] = storageService.findBB(_, _, _)(dataEC)
      val denormalisationFlow = RelationFlow.denormalizeRelationFlow(mapRef, toData)(denormalisingEC, materializer)
      UtilityFlows.filterRelation.via(denormalisationFlow)
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

}
