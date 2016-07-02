package io.plasmap.geo.preprocessing.test

import _root_.io.plasmap.generator.OsmObjectGenerator
import _root_.io.plasmap.geo.preprocessing.OsmPreprocessor._
import _root_.io.plasmap.util.test.OsmTestData
import akka.NotUsed
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Flow, Source}
import io.plasmap.geo.data.OsmBB
import io.plasmap.geo.mappings.{OsmMapping, OsmNodeMapping, OsmRelationMapping, OsmWayMapping}
import io.plasmap.geo.preprocessing.{FlowError, OsmPreprocessor, RelationFlow}
import io.plasmap.model.geometry.{GeometryCollection, HashPoint, LineString, Point}
import io.plasmap.model._
import io.plasmap.util.Denormalizer
import org.joda.time.DateTime
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scalaz.{Disjunction, Sink => _, Source => _}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

/**
  * Specification for Queries
  */
class RelationFlowSpec
  extends Specification
    with IsolatedMockFactory
    with ProxyMockFactory {

  sequential

  val system = ActorSystem("test")
  val mat = ActorMaterializer()

  val gen = OsmObjectGenerator()

  import TestFixtures._

  "The OsmPreprocessor" should {

    "denormalize a relation" in {

      val genNodeData: List[((OsmId, Long), ((Long, OsmId, OsmType), HashPoint), (OsmId, Point))] = for {
        relation <- relations
        ref <- relation.refs if ref.typ == OsmTypeNode
        id = ref.ref
        hash: Long = gen.generatePoint.hash
        point = Point(hash)
      } yield ((id, hash), ((hash, id, ref.typ), point), (id, point))

      val nodeMappings: Map[OsmId, Long] = genNodeData.map(_._1).toMap
      val nodeData: Map[(Long, OsmId, OsmType), HashPoint] = genNodeData.map(_._2).toMap
      val nodeRefs: Map[OsmId, Point] = genNodeData.map(_._3).toMap

      val genWayData: List[((OsmId, Long), ((Long, OsmId, OsmType), LineString), (OsmId, LineString))] = for {
        relation <- relations
        ref <- relation.refs if ref.typ == OsmTypeWay
        id = ref.ref
        hash: Long = gen.generatePoint.hash
        lineString = gen.generateLinestring
      } yield ((id, hash), ((hash, id, ref.typ), lineString), (id, lineString))

      val wayMappings: Map[OsmId, Long] = genWayData.map(_._1).toMap
      val wayData: Map[(Long, OsmId, OsmType), LineString] = genWayData.map(_._2).toMap
      val wayRefs: Map[OsmId, LineString] = genWayData.map(_._3).toMap

      val genRelationData: List[((OsmId, Long), ((Long, OsmId, OsmType), GeometryCollection), (OsmId, GeometryCollection))] = for {
        relation <- relations
        ref <- relation.refs if ref.typ == OsmTypeRelation
        id = ref.ref
        hash: Long = gen.generatePoint.hash
        geometryCollection = gen.generateGeometryCollection
      } yield ((id, hash), ((hash, id, ref.typ), geometryCollection), (id, geometryCollection))

      val relationMappings: Map[OsmId, Long] = genRelationData.map(_._1).toMap
      val relationData: Map[(Long, OsmId, OsmType), GeometryCollection] = genRelationData.map(_._2).toMap
      val relationRefs: Map[OsmId, GeometryCollection] = genRelationData.map(_._3).toMap

      val expectedRelationsF = (for {
        relation <- relations
      } yield relation -> gen.generateDenormalizedRelation).toMap

      val expectedRelations = expectedRelationsF.toList.map(_._2)

      val numRefs = relations.foldLeft(0)((acc, relation) => relation.refs.size + acc)

      val mappingF = mockFunction[OsmId, OsmType, Future[Option[OsmMapping]]]
      mappingF expects(*, *) onCall { (id: OsmId, typ: OsmType) => typ match {
        case OsmTypeNode => Future {
          nodeMappings.get(id).map(OsmNodeMapping(_, id, DateTime.now()))
        }
        case OsmTypeWay => Future {
          wayMappings.get(id).map(OsmWayMapping(_, id, DateTime.now()))
        }
        case OsmTypeRelation => Future {
          relationMappings.get(id).map(OsmRelationMapping(_, id, DateTime.now()))
        }
      }
      } repeat numRefs

      val dataF = mockFunction[Long, OsmId, OsmType, Future[Option[OsmBB]]]
      dataF expects(*, *, *) onCall { (bb: Long, id: OsmId, typ: OsmType) => typ match {
        case OsmTypeNode =>
          val key = (bb, id, typ)
          val pointOpt: Option[Point] = nodeData.get(key)
          Future {
            pointOpt.map((point) => {
              val node = OsmDenormalizedNode(id = id, tags = List.empty[OsmTag], geometry = point)
              OsmBB(bb, id, node)
            })
          }
        case OsmTypeWay =>
          val key = (bb, id, typ)
          val lineStringOpt: Option[LineString] = wayData.get(key)
          Future {
            lineStringOpt.map((lineString) => {
              val way = OsmDenormalizedWay(id = id, tags = List.empty[OsmTag], geometry = lineString)
              OsmBB(bb, id, way)
            })
          }
        case OsmTypeRelation =>
          val key = (bb, id, typ)
          val geometryCollectionOpt: Option[GeometryCollection] = relationData.get(key)
          Future {
            geometryCollectionOpt.map((geometryCollection) => {
              val relation = OsmDenormalizedRelation(id = id, tags = List.empty[OsmTag], geometry = geometryCollection)
              OsmBB(bb, id, relation)
            })
          }
      }
      } repeat numRefs

      val denormaliseF: (OsmRelation, Map[OsmId, Point], Map[OsmId, LineString], Map[OsmId, GeometryCollection]) => OsmDenormalizedRelation
      = (rel, a, b, c) => expectedRelationsF(rel)

      val relationFlow: Flow[OsmRelation, Disjunction[FlowError, OsmDenormalizedRelation], NotUsed] = RelationFlow.denormalizeRelationFlow(
        mapRef = mappingF,
        toData = dataF,
        denormalizeRelation = denormaliseF
      )

      val eventualDenormalizedRelsFut: Future[List[Disjunction[FlowError, OsmDenormalizedRelation]]] =
        Source(relations)
          .via(relationFlow)
          .runFold(List.empty[Disjunction[FlowError, OsmDenormalizedRelation]])((list, drelation: Disjunction[FlowError, OsmDenormalizedRelation]) => drelation :: list)

      import scalaz._, Scalaz._

      val eventualDenormalizedRelations: List[Disjunction[FlowError, OsmDenormalizedRelation]] = Await.result(eventualDenormalizedRelsFut, 10 seconds)

      type ExOr[A] = FlowError \/ A

      val actualDRelations: FlowError \/ List[OsmDenormalizedRelation] = eventualDenormalizedRelations.sequence[ExOr, OsmDenormalizedRelation]
      println(s"Size: ${expectedRelations.size}")
      actualDRelations.getOrElse(List.empty) must containAllOf(expectedRelations)

    }
  }

}
