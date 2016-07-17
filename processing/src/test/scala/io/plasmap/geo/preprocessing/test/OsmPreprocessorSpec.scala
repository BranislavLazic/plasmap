package io.plasmap.geo.preprocessing.test

import _root_.io.plasmap.generator.OsmObjectGenerator
import _root_.io.plasmap.geo.data.{OsmBB, OsmBBTag}
import _root_.io.plasmap.geo.mappings._
import _root_.io.plasmap.geo.preprocessing.OsmPreprocessor
import _root_.io.plasmap.geo.preprocessing.OsmPreprocessor._
import _root_.io.plasmap.model._
import _root_.io.plasmap.model.geometry.{HashPoint, GeometryCollection, LineString, Point}
import _root_.io.plasmap.parser.OsmParser
import _root_.io.plasmap.util.Denormalizer
import _root_.io.plasmap.util.test.OsmTestData
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Source, _}
import org.joda.time.DateTime
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.Scalaz._
import scalaz.{Sink => _, Source => _, _}

/**
 * Specification for Queries
 */
class OsmPreprocessorSpec
  extends Specification
  with IsolatedMockFactory
  with ProxyMockFactory {

  sequential

  val system = ActorSystem("test")
  val mat = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global


  //val mappings: Map[OsmId, Point] = elements.collect { case node: OsmNode => node }.map((node) => node.id -> Point(node.point.hash)).toMap

  val gen = OsmObjectGenerator()

   //"The OsmPreprocessor" should {


     /*"persist mappings - success case" in {

      val toMapping = mockFunction[OsmDenormalizedObject, OsmMapping]
      val storeMapping = mockFunction[OsmMapping, Future[Option[OsmMapping]]]

      val node = gen.generateDenormalizedNode
      val nodeMapping = OsmNodeMapping(10L, node.id, DateTime.now)

      val way = gen.generateDenormalizedWay
      val wayMapping = OsmWayMapping(20L, way.id, DateTime.now)

      val rel = gen.generateDenormalizedRelation
      val relMapping = OsmRelationMapping(30L, rel.id, DateTime.now)

      inAnyOrder {
        toMapping expects node returns nodeMapping once()
        toMapping expects way returns wayMapping once()
        toMapping expects rel returns relMapping once()
      }

      inAnyOrder {
        storeMapping expects nodeMapping returns Future {
          Some(nodeMapping)
        } once()
        storeMapping expects wayMapping returns Future {
          Some(wayMapping)
        } once()
        storeMapping expects relMapping returns Future {
          Some(relMapping)
        } once()
      }

      val source = Source(List(node, way, rel))

      val mappingFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistMappingFlow(toMapping, storeMapping)
      val flow = source.via(mappingFlow)


      val expectedElements: List[OsmDenormalizedObject] = List(node, way, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      Await.result(actualElementsFut, 100 millis) must containTheSameElementsAs(expectedElements.map(_.id.successNel[OsmDenormalizedObject]))
    }

    "persist mappings - failure case" in {

      val toMapping = mockFunction[OsmDenormalizedObject, OsmMapping]
      val storeMapping = mockFunction[OsmMapping, Future[Option[OsmMapping]]]

      val node = gen.generateDenormalizedNode
      val nodeMapping = OsmNodeMapping(10L, node.id, DateTime.now)

      val way = gen.generateDenormalizedWay
      val wayMapping = OsmWayMapping(20L, way.id, DateTime.now)

      val rel = gen.generateDenormalizedRelation
      val relMapping = OsmRelationMapping(30L, rel.id, DateTime.now)

      inAnyOrder {
        toMapping expects node returns nodeMapping once()
        toMapping expects way returns wayMapping once()
        toMapping expects rel returns relMapping once()
      }

      inAnyOrder {
        storeMapping expects nodeMapping returns Future {
          None
        } once()
        storeMapping expects wayMapping returns Future {
          None
        } once()
        storeMapping expects relMapping returns Future {
          None
        } once()
      }

      val source = Source(List(node, way, rel))

      val mappingFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistMappingFlow(toMapping, storeMapping)
      val flow = source.via(mappingFlow)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      val expectedElements: List[OsmDenormalizedObject] = List(node, way, rel)
      Await.result(actualElementsFut, 100 millis) must containTheSameElementsAs(expectedElements
        .map((obj) => PersisterError(obj.id, MappingPersisterStep).failureNel[OsmId]))
    }


    "persist index - success case" in {

      val toIndex = mockFunction[OsmDenormalizedObject, IndexMapping]
      val storeIndex = mockFunction[IndexMapping, Future[Option[IndexMapping]]]

      val node = gen.generateDenormalizedNode
      val nodeTags = Map("amenity" -> "restaurant", "street" -> "main str.")
      val nodeIndex = IndexMapping(node, nodeTags)

      val way = gen.generateDenormalizedWay
      val wayTags = Map.empty[String, String]
      val wayIndex = IndexMapping(way, wayTags)

      val rel = gen.generateDenormalizedRelation
      val relTags = Map("boundary" -> "administrative")
      val relIndex = IndexMapping(rel, relTags)

      inAnyOrder {
        toIndex expects node returns nodeIndex once()
        toIndex expects way returns wayIndex once()
        toIndex expects rel returns relIndex once()
      }

      inAnyOrder {
        storeIndex expects nodeIndex returns Future {
          Some(nodeIndex)
        } once()
        storeIndex expects wayIndex returns Future {
          None
        } never()
        storeIndex expects relIndex returns Future {
          Some(relIndex)
        } once()
      }

      val source = Source(List(node, way, rel))

      val indexFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistIndexFlow(toIndex, storeIndex)
      val flow = source.via(indexFlow)

      val expectedElements: List[OsmDenormalizedObject] = List(node, way, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      Await.result(actualElementsFut, 100 millis) must containTheSameElementsAs(expectedElements.map(_.id.successNel[OsmDenormalizedObject]))
    }

    "persist index - failure case" in {

      val toIndex = mockFunction[OsmDenormalizedObject, IndexMapping]
      val storeIndex = mockFunction[IndexMapping, Future[Option[IndexMapping]]]

      val node = gen.generateDenormalizedNode
      val nodeTags = Map("amenity" -> "restaurant", "street" -> "main str.")
      val nodeIndex = IndexMapping(node, nodeTags)

      val way = gen.generateDenormalizedWay
      val wayTags = Map.empty[String, String]
      val wayIndex = IndexMapping(way, wayTags)

      val rel = gen.generateDenormalizedRelation
      val relTags = Map("boundary" -> "administrative")
      val relIndex = IndexMapping(rel, relTags)

      inAnyOrder {
        toIndex expects node returns nodeIndex once()
        toIndex expects way returns wayIndex once()
        toIndex expects rel returns relIndex once()
      }

      inAnyOrder {
        storeIndex expects nodeIndex returns Future {
          None
        } once()
        storeIndex expects wayIndex returns Future {
          None
        } never()
        storeIndex expects relIndex returns Future {
          None
        } once()
      }

      val source = Source(List(node, way, rel))

      val indexFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistIndexFlow(toIndex, storeIndex)
      val flow = source.via(indexFlow)

      val expectedElements: List[OsmDenormalizedObject] = List(node, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      Await.result(actualElementsFut, 100 millis) must containAllOf(expectedElements
        .map((obj) => PersisterError(obj.id, IndexPersisterStep).failureNel[OsmId]))
    }

    "persist data by bb - success case" in {

      val toBB = mockFunction[OsmDenormalizedObject, OsmBB]
      val storeOsmBB = mockFunction[OsmBB, Future[Option[OsmBB]]]

      val node = gen.generateDenormalizedNode
      val nodeBB = OsmBB(10L, node.id, node)

      val way = gen.generateDenormalizedWay
      val wayBB = OsmBB(20L, way.id, way)

      val rel = gen.generateDenormalizedRelation
      val relBB = OsmBB(30L, rel.id, rel)

      inAnyOrder {
        toBB expects node returns nodeBB once()
        toBB expects way returns wayBB once()
        toBB expects rel returns relBB once()
      }

      inAnyOrder {
        storeOsmBB expects nodeBB returns Future {
          Some(nodeBB)
        } once()
        storeOsmBB expects wayBB returns Future {
          Some(wayBB)
        } once()
        storeOsmBB expects relBB returns Future {
          Some(relBB)
        } once()
      }

      val source = Source(List(node, way, rel))

      val storeFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistDataFlow(toBB, storeOsmBB)
      val flow = source.via(storeFlow)

      val expectedElements: List[OsmDenormalizedObject] = List(node, way, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmDenormalizedObject]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      Await.result(actualElementsFut, 100 millis) must containTheSameElementsAs(expectedElements.map(_.id.successNel[OsmDenormalizedObject]))
    }

    "persist data by bb - failure case" in {

      val toBB = mockFunction[OsmDenormalizedObject, OsmBB]
      val storeOsmBB = mockFunction[OsmBB, Future[Option[OsmBB]]]

      val node = gen.generateDenormalizedNode
      val nodeBB = OsmBB(10L, node.id, node)

      val way = gen.generateDenormalizedWay
      val wayBB = OsmBB(20L, way.id, way)

      val rel = gen.generateDenormalizedRelation
      val relBB = OsmBB(30L, rel.id, rel)

      inAnyOrder {
        toBB expects node returns nodeBB once()
        toBB expects way returns wayBB once()
        toBB expects rel returns relBB once()
      }

      inAnyOrder {
        storeOsmBB expects nodeBB returns Future {
          None
        } once()
        storeOsmBB expects wayBB returns Future {
          None
        } once()
        storeOsmBB expects relBB returns Future {
          None
        } once()
      }

      val source = Source(List(node, way, rel))

      val storeFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistDataFlow(toBB, storeOsmBB)
      val flow = source.via(storeFlow)

      val expectedElements: List[OsmDenormalizedObject] = List(node, way, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      Await.result(actualElementsFut, 100 millis) must containTheSameElementsAs(expectedElements
        .map((obj) => PersisterError(obj.id, DataPersisterStep).failureNel[OsmId]))
    }

    "persist data by bb & tag - success case" in {

      val toBBTag = mockFunction[OsmDenormalizedObject, List[OsmBBTag]]
      val storeOsmBBTag = mockFunction[OsmBBTag, Future[Option[OsmBBTag]]]

      val node = gen.generateDenormalizedNode
      val nodeBB1 = OsmBBTag(10L, node.id, OsmTag("amenity", "restaurant"), node)
      val nodeBB2 = OsmBBTag(10L, node.id, OsmTag("cuisine", "italian"), node)
      val nodeBB3 = OsmBBTag(10L, node.id, OsmTag("opening_hours", "Mo-Fr 09:00-18:00"), node)

      val way = gen.generateDenormalizedWay

      val rel = gen.generateDenormalizedRelation
      val relBB = OsmBBTag(10L, rel.id, OsmTag("amenity", "restaurant"), rel)

      inAnyOrder {
        toBBTag expects node returns List(nodeBB1, nodeBB2, nodeBB3) once()
        toBBTag expects way returns List() once()
        toBBTag expects rel returns List(relBB) once()
      }

      inAnyOrder {
        storeOsmBBTag expects nodeBB1 returns Future {
          Some(nodeBB1)
        } once()
        storeOsmBBTag expects nodeBB2 returns Future {
          Some(nodeBB2)
        } once()
        storeOsmBBTag expects nodeBB3 returns Future {
          Some(nodeBB3)
        } once()
        storeOsmBBTag expects relBB returns Future {
          Some(relBB)
        } once()
      }

      val source = Source(List(node, way, rel))

      val storeFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistDataByTagFlow(toBBTag, storeOsmBBTag)
      val flow = source.via(storeFlow)

      val expectedElements: List[OsmDenormalizedObject] = List(node, way, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      Await.result(actualElementsFut, 100 millis) must containTheSameElementsAs(expectedElements.map(_.id.successNel[OsmId]))
    }

    "persist data by bb & tag - failure case" in {

      val toBBTag = mockFunction[OsmDenormalizedObject, List[OsmBBTag]]
      val storeOsmBBTag = mockFunction[OsmBBTag, Future[Option[OsmBBTag]]]

      val node = gen.generateDenormalizedNode
      val nodeBB1 = OsmBBTag(10L, node.id, OsmTag("amenity", "restaurant"), node)
      val nodeBB2 = OsmBBTag(10L, node.id, OsmTag("cuisine", "italian"), node)
      val nodeBB3 = OsmBBTag(10L, node.id, OsmTag("opening_hours", "Mo-Fr 09:00-18:00"), node)

      val way = gen.generateDenormalizedWay

      val rel = gen.generateDenormalizedRelation
      val relBB = OsmBBTag(10L, rel.id, OsmTag("amenity", "restaurant"), rel)

      inAnyOrder {
        toBBTag expects node returns List(nodeBB1, nodeBB2, nodeBB3) once()
        toBBTag expects way returns List() once()
        toBBTag expects rel returns List(relBB) once()
      }

      inAnyOrder {
        storeOsmBBTag expects nodeBB1 returns Future {
          Some(nodeBB1)
        } once()
        storeOsmBBTag expects nodeBB2 returns Future {
          None
        } once()
        storeOsmBBTag expects nodeBB3 returns Future {
          Some(nodeBB3)
        } once()
        storeOsmBBTag expects relBB returns Future {
          None
        } once()
      }

      val source = Source(List(node, way, rel))

      val storeFlow: Flow[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId], Unit] = OsmPreprocessor.createPersistDataByTagFlow(toBBTag, storeOsmBBTag)
      val flow = source.via(storeFlow)

      val expectedElements: List[OsmDenormalizedObject] = List(node, rel)
      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = flow.runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      val result: List[ValidationNel[PersisterError, OsmId]] = Await.result(actualElementsFut, 100 millis)
      result must containAllOf(expectedElements
        .map((obj) => PersisterError(obj.id, DataByTagPersisterStep).failureNel[OsmId]))
      result must contain(way.successNel[PersisterError])
    }

    "persist elements individually" in {

      val mappingFlowF = mockFunction[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId]]
      val dataFlowF = mockFunction[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId]]
      val dataByTagFlowF = mockFunction[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId]]
      val indexingFlowF = mockFunction[OsmDenormalizedObject, ValidationNel[PersisterError, OsmId]]

      val mappingFlow = Flow[OsmDenormalizedObject].map(mappingFlowF)
      val dataFlow = Flow[OsmDenormalizedObject].map(dataFlowF)
      val dataByTagFlow = Flow[OsmDenormalizedObject].map(dataByTagFlowF)
      val indexingFlow = Flow[OsmDenormalizedObject].map(indexingFlowF)

      val nodes: List[OsmDenormalizedObject] = List.fill(3)(gen.generateDenormalizedNode)
      val ways: List[OsmDenormalizedObject] = List.fill(1)(gen.generateDenormalizedWay)
      val rels: List[OsmDenormalizedObject] = List.empty //List(1 to 2).map(_ => gen.generateDenormalizedRelation)

      /* Node 0 fail on indexing */
      val error_0_0: ValidationNel[PersisterError, OsmId] = PersisterError(nodes(0).id, IndexPersisterStep).failureNel[OsmId]

      /* Node 1 fail on mapping & data */
      val error_1_0: ValidationNel[PersisterError, OsmId] = PersisterError(nodes(1).id, DataPersisterStep).failureNel[OsmId]
      val error_1_1: ValidationNel[PersisterError, OsmId] = PersisterError(nodes(1).id, IndexPersisterStep).failureNel[OsmId]
      val errors_1: List[ValidationNel[PersisterError, OsmId]] = List(error_1_0, error_1_1)

      /* Node 2 succeed completely */
      val successes_2: ValidationNel[PersisterError, OsmId] = nodes(2).id.successNel[PersisterError]

      /* Way 1 fail on mapping & data */
      val error_4_0: ValidationNel[PersisterError, OsmId] = PersisterError(ways(0).id, MappingPersisterStep).failureNel[OsmId]
      val error_4_1: ValidationNel[PersisterError, OsmId] = PersisterError(ways(0).id, DataPersisterStep).failureNel[OsmId]
      val error_4_2: ValidationNel[PersisterError, OsmId] = PersisterError(ways(0).id, DataByTagPersisterStep).failureNel[OsmId]
      val error_4_3: ValidationNel[PersisterError, OsmId] = PersisterError(ways(0).id, IndexPersisterStep).failureNel[OsmId]
      val errors_4: List[ValidationNel[PersisterError, OsmId]] = List(error_4_0, error_4_1, error_4_2, error_4_3)


      inAnyOrder {

        mappingFlowF expects nodes(0) returns nodes(0).successNel[PersisterError] once()
        dataFlowF expects nodes(0) returns nodes(0).successNel[PersisterError] once()
        dataByTagFlowF expects nodes(0) returns nodes(0).successNel[PersisterError] once()
        indexingFlowF expects nodes(0) returns error_0_0 once()

        mappingFlowF expects nodes(1) returns nodes(1).successNel[PersisterError] once()
        dataFlowF expects nodes(1) returns error_1_0 once()
        dataByTagFlowF expects nodes(1) returns nodes(1).successNel[PersisterError] once()
        indexingFlowF expects nodes(1) returns error_1_1 once()

        mappingFlowF expects nodes(2) returns successes_2 once()
        dataFlowF expects nodes(2) returns successes_2 once()
        dataByTagFlowF expects nodes(2) returns successes_2 once()
        indexingFlowF expects nodes(2) returns successes_2 once()

        mappingFlowF expects ways(0) returns error_4_0 once()
        dataFlowF expects ways(0) returns error_4_1 once()
        dataByTagFlowF expects ways(0) returns error_4_2 once()
        indexingFlowF expects ways(0) returns error_4_3 once()

      }

      val source = Source(nodes ++ ways ++ rels)

      val persisterFlow: Flow[OsmDenormalizedObject, Validation[NonEmptyList[PersisterError], OsmId], Unit] = OsmPreprocessor.createPersisterFlow(mappingFlow, dataFlow, dataByTagFlow, indexingFlow)

      val actualElementsFut: Future[List[ValidationNel[PersisterError, OsmId]]] = source.via(persisterFlow)
        .runFold(List.empty[ValidationNel[PersisterError, OsmId]])((list, elem: ValidationNel[PersisterError, OsmId]) => elem :: list)

      val result: List[ValidationNel[PersisterError, OsmId]] = Await.result(actualElementsFut, 1000 millis)

      def toErrors(result: List[ValidationNel[PersisterError, OsmId]]): Set[PersisterError] = {
        result.map(_.leftMap(_.toSet).map(_ => Set.empty[PersisterError]).disjunction.merge).sequenceU
      }

      val set: Set[PersisterError] = toErrors(result)

      result must contain(error_0_0)
      set must containAllOf(toErrors(errors_1).toSeq)
      result must contain(successes_2)
      set must containAllOf(toErrors(errors_4).toSeq)
    }*/
   //}

}
