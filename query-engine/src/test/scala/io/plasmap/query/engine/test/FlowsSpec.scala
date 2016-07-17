package io.plasmap.query.engine.test

import _root_.io.plasmap.query.engine._
import _root_.io.plasmap.querymodel.{PMCityFromCoordinates, PMCoordinates, PMSerialiser}
import _root_.io.plasmap.serializer.OsmDenormalizedSerializer
import akka.NotUsed
import akka.actor._
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
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

with IsolatedMockFactory
with ProxyMockFactory {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  import io.plasmap.util.test.OsmTestData._

  "The Flows" should {
    "extract and execute a City Query" in {

      val toQuery = stubFunction[String, Source[String, NotUsed]]

      val geoJsonCities: List[String] = List(duisburg, essen, muehlheim)
        .map(City)
        .map((city: City) => OsmDenormalizedSerializer.toGeoJsonString(city.osmObject))
      val serializedSource: Source[String, NotUsed] = Source(geoJsonCities)

      val pmCity = PMCityFromCoordinates(PMCoordinates(10, 20))
      val serialised: String = PMSerialiser.serialiseQuery(pmCity)
      val message = TextMessage(serialised)

      toQuery when * returns serializedSource

      val query: Flow[Message, Message, NotUsed] = Flows.query(toQuery)
      val fut: Future[List[Message]] = Source.single(message).via(query).runFold(List.empty[Message])((list,msg) => msg :: list)
      val actual = Await.result(fut,1000 millis)

      val expected = geoJsonCities.map(TextMessage(_))
      actual must containAllOf(expected)
    }

  }

}
