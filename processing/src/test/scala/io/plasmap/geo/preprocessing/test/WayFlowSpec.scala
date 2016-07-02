package io.plasmap.geo.preprocessing.test

import _root_.io.plasmap.generator.OsmObjectGenerator
import _root_.io.plasmap.geo.data.OsmBB
import _root_.io.plasmap.geo.mappings._
import _root_.io.plasmap.geo.preprocessing.{FlowError, OsmPreprocessor, WayFlow}
import _root_.io.plasmap.geo.preprocessing.OsmPreprocessor._
import _root_.io.plasmap.model._
import _root_.io.plasmap.model.geometry.{GeometryCollection, HashPoint, LineString, Point}
import _root_.io.plasmap.util.Denormalizer
import _root_.io.plasmap.util.test.OsmTestData
import akka.NotUsed
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Source, _}
import org.joda.time.DateTime
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.{Sink => _, Source => _, _}

/**
 * Specification for Queries
 */
class WayFlowSpec
  extends Specification
  with IsolatedMockFactory
  with ProxyMockFactory {

  sequential

  val system = ActorSystem("test")
  val mat = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global


  import TestFixtures._

  val gen = OsmObjectGenerator()

  "The WayFlow" should {

    "denormalize a way" in {

      val mappings: Map[OsmId, Point] = (for {
        way <- ways
        nd <- way.nds
        point = Point(gen.generatePoint.hash)
      } yield nd -> point).toMap

      val expectedWays = for {
        way <- ways
      } yield Denormalizer.denormalizeWay(way, mappings)

      val numNds = ways.foldLeft(0)((acc, way) => way.nds.size + acc)

      val mappingF = mockFunction[OsmId, Future[Option[OsmNodeMapping]]]
      mappingF expects * onCall { id: OsmId =>
        val mappingOpt: Option[Point] = mappings.get(id)
        Future {
          mappingOpt.map((mapping) => {
            OsmNodeMapping(mapping.hash, id, DateTime.now)
          })
        }
      } repeat numNds


      val wayFlow: Flow[OsmWay, Disjunction[FlowError, OsmDenormalizedWay], NotUsed] = WayFlow.denormalizeWayFlow(mappingF)

      val eventualDenormalizedWaysFut: Future[List[OsmDenormalizedWay]] =
        Source(ways)
          .via(wayFlow)
          .filter(_.isRight)
          .map(_.toOption.get)
          .runFold(List.empty[OsmDenormalizedWay])((list, dway: OsmDenormalizedWay) => dway :: list)

      val eventualDenormalizedWays: List[OsmDenormalizedWay] = Await.result(eventualDenormalizedWaysFut, 10 seconds)
      eventualDenormalizedWays must containAllOf(expectedWays)
    }
  }
}
