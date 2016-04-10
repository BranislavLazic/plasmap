package io.plasmap.query.engine.test

import _root_.io.plasmap.query.engine._
import _root_.io.plasmap.querymodel.{PMCoordinates, PMCityFromCoordinates, PMSerialiser}
import _root_.io.plasmap.serializer.OsmDenormalizedSerializer
import akka.actor._
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{TextMessage, BinaryMessage, Message}
import akka.stream.FanInShape.{Init => InInit, Name => FISName}
import akka.stream.FanOutShape.{Init => OutInit, Name => FOSName}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Specification for OsmWayPointMapper
 */
class FlowsSpec extends Specification
with OsmTestData
with IsolatedMockFactory
with ProxyMockFactory {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  "The Flows" should {
    "extract and execute a City Query" in {

      val toQuery = stubFunction[String, Source[String, Unit]]

      val geoJsonCities: List[String] = List(duisburg, essen, muehlheim)
        .map(City)
        .map((city: City) => OsmDenormalizedSerializer.toGeoJsonString(city.osmObject))
      val serializedSource: Source[String, Unit] = Source(geoJsonCities)

      val pmCity = PMCityFromCoordinates(PMCoordinates(10, 20))
      val serialised: String = PMSerialiser.serialiseQuery(pmCity)
      val message = TextMessage(serialised)

      toQuery when * returns serializedSource

      val query: Flow[Message, TextMessage, Unit] = Flows.query(toQuery)
      val fut: Future[List[TextMessage]] = Source.single(message).via(query).runFold(List.empty[TextMessage])((list,msg) => msg :: list)
      val actual = Await.result(fut,1000 millis)

      val expected = geoJsonCities.map(TextMessage(_))
      actual must containAllOf(expected)
    }
    "test" in {
      val source = Source(List(1,2,3))
      val addFlow = Flow[Int].map(_+1)
      val stringFlow = Flow[Int].map(_.toString)
      val sink = Sink.foreach(println)
      source.via(addFlow).via(stringFlow).to(sink).run()

      true must beTrue
    }
  }

}
