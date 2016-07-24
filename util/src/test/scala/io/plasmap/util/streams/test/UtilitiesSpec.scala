package io.plasmap.util.streams.test

import _root_.io.plasmap.util.streams.Utilities
import akka.NotUsed
import akka.actor._
import akka.stream.FanInShape.{Init => InInit, Name => FISName}
import akka.stream.FanOutShape.{Init => OutInit, Name => FOSName}
import akka.stream._
import akka.stream.scaladsl._
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Specification for OsmWayPointMapper
  */
class UtilitiesSpec extends Specification {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()


  "The Utilities" should {

    /*"map and group asynchroneous functions" in {

      val source: Source[Int, NotUsed] = Source(List(1, 2, 3, 4))
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
    }*/

    "add groupkey to a flow" in {

      val source = Source.single(1)
      val flow = Flow[Int]
        .mapConcat(_ => List(1, 2, 3, 4, 5))
        .map(_ + 10)
      val subFlow = Utilities.subflowWithGroupKey[Int,Int,Int](identity,flow)

      val expected = List(
        1 -> 11,
        1 -> 12,
        1 -> 13,
        1 -> 14,
        1 -> 15
      )

      val future = source.via(subFlow).runFold(List.empty[(Int, Int)])((elems, elem) => elem :: elems)
      val result = Await.result(future, 100 millis)
      result must containTheSameElementsAs(expected)

    }

    "run a flow on a grouped subflow" in {

      val source = Source(1 to 3)
      val subFlow = Flow[Int]
          .mapConcat {
            case 1 => List("a1", "b1", "c1")
            case 2 => List("d2", "e2", "f2")
            case 3 => List("g3", "h3", "i3")
          }

      val expected = List(
        1 -> "a1",
        1 -> "b1",
        1 -> "c1",
        2 -> "d2",
        2 -> "e2",
        2 -> "f2",
        3 -> "g3",
        3 -> "h3",
        3 -> "i3"
      )

      val subFlowWithGroupKey = Utilities.subflowWithGroupKey[Int,Int,String](identity _,subFlow)

      val flow: Flow[Int, (Int, String), NotUsed] = Utilities.groupAndMapSubFlow[Int,Int,(Int,String)](identity,subFlowWithGroupKey,100)

      val future = source.via(flow).runFold(List.empty[(Int, String)])((elems, elem) => elem :: elems)
      val result = Await.result(future, 100 millis)
      result must containTheSameElementsAs(expected)

    }

    "group, map, and flatten a stream" in {

         val source: Source[Int, NotUsed] = Source(List(1, 2, 3, 4, 3, 2, 4, 1, 2, 3, 4, 2, 2, 1, 3, 2, 4, 1))

         val subflow1: Flow[Int, String, NotUsed] = Flow[Int].map {
           case 1 => "a"
           case 2 => "b"
           case 3 => "c"
           case 4 => "d"
           case _ => ""
         }

         val subflow2: Flow[String, String, NotUsed] = Flow[String].map {
           case "a" => "I"
           case "b" => "II"
           case "c" => "III"
           case "d" => "IV"
           case _ => ""
         }

         val groupFlow = Utilities.groupAndMapSubflowWithKey(identity[Int], Flow[Int].via(subflow1).via(subflow2), 400)
         val groupSource = source.via(groupFlow)

         val listFut: Future[List[(Int, String)]] = groupSource.runFold(List.empty[(Int, String)])((list, element: (Int, String)) => element :: list)
         val list = Await.result(listFut, 100 millis)

         list.toSet must containTheSameElementsAs(List((1, "I"), (2, "II"), (3, "III"), (4, "IV")))

       }
  }

}
