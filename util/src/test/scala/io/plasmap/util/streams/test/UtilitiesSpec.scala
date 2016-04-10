package io.plasmap.util.streams.test

import _root_.io.plasmap.util.streams.Utilities
import akka.actor._
import akka.stream.FanInShape.{Init => InInit, Name => FISName}
import akka.stream.FanOutShape.{Init => OutInit, Name => FOSName}
import akka.stream._
import akka.stream.scaladsl._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Specification for OsmWayPointMapper
 */
class UtilitiesSpec extends Specification {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()


  "The Utilities" should {
    "map and group asynchroneous functions" in {

      val source: Source[Int, Unit] = Source(List(1, 2, 3, 4))
      val sink = Sink.ignore

      def toElements(input: Int): List[String] = {
        val dictionary = Map(
          1 -> List("a1", "b1", "c1"),
          2 -> List("d2", "e2", "f2"),
          3 -> List("g3", "h3", "i3"))
        dictionary.getOrElse(input, List.empty[String])
      }
      def toElementsAsync(input: Int): Future[List[String]] = {
        val dictionary = Map(
          1 -> List("a1", "b1", "c1"),
          2 -> List("d2", "e2", "f2"),
          3 -> List("g3", "h3", "i3"))
        Future {
          dictionary.getOrElse(input, List.empty[String])
        }
      }

      val mappedSource: Source[(Int, String), Unit] = Utilities.mapConcatAndGroupAsync(source, toElementsAsync)
      mappedSource.runWith(sink)

      // TODO: do proper checking here
      true must beTrue
    }

    "group, map, and flatten a stream" in {

      val source: Source[Int, Unit] = Source(List(1, 2, 3, 4, 3, 2, 4, 1, 2, 3, 4, 2, 2, 1, 3, 2, 4, 1))

      val subflow1: Flow[Int, String, Unit] = Flow[Int].map {
        case 1 => "a"
        case 2 => "b"
        case 3 => "c"
        case 4 => "d"
        case _ => ""
      }

      val subflow2: Flow[String, String, Unit] = Flow[String].map {
        case "a" => "I"
        case "b" => "II"
        case "c" => "III"
        case "d" => "IV"
        case _ => ""
      }

      val groupFlow: Flow[Int, (Int, List[String]), Unit] = Utilities.groupAndMap(400, identity[Int], Flow[Int].via(subflow1).via(subflow2))
      val groupSource: Source[(Int, List[String]), Unit] = source.via(groupFlow)
      val flattenedSource: Source[(Int, String), Unit] = Utilities.flatten(groupSource)

      val listFut: Future[List[(Int, String)]] = flattenedSource.runFold(List.empty[(Int, String)])((list, element: (Int, String)) => element :: list)
      val list = Await.result(listFut, 100 millis)

      list.toSet must containTheSameElementsAs(List((1, "I"), (2, "II"), (3, "III"), (4, "IV")))

    }
  }

}
