package io.plasmap.util.streams.test

import _root_.io.plasmap.util.streams.StreamAdditions
import akka.actor._
import akka.stream.FanInShape.{Init => InInit, Name => FISName}
import akka.stream.FanOutShape.{Init => OutInit, Name => FOSName}
import akka.stream._
import akka.stream.scaladsl._
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

/**
 * Specification for OsmWayPointMapper
 */
class StreamAdditionsSpec extends Specification {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  import StreamAdditions._

  "The FlowAdditions" should {

    val totalNumbers = 5000000
    val distinctNumbers = 10000
    val sampleSize = 50000

    "deduplicate a source" in {

      val elements = Seq.fill(totalNumbers)(Random.nextInt(distinctNumbers)).toList

      val eventualDeduplicated =
        Source(elements)
        .deduplicate(distinctNumbers, 0.001)
        .runFold(List.empty[Int])((acc, item) => item :: acc)

      val deduplicated = Await.result(eventualDeduplicated, 10 seconds)
      deduplicated must haveSize(distinctNumbers)
      deduplicated must containTheSameElementsAs(elements.distinct)
    }

    "deduplicate a flow" in {

      val elements = Seq.fill(totalNumbers)(Random.nextInt(distinctNumbers)).toList

      val deduplicator = Flow[Int].deduplicate(distinctNumbers,0.001)
      val eventualDeduplicated = Source(elements)
        .via(deduplicator)
        .runFold(List.empty[Int])((acc, item) => item :: acc)

      val deduplicated = Await.result(eventualDeduplicated, 10 seconds)
      deduplicated must haveSize(distinctNumbers)
      deduplicated must containTheSameElementsAs(elements.distinct)
    }

    "count the distinct elements of a source" in {

      val elements:List[Long] = Random.shuffle(for { i <- 0 to totalNumbers } yield Random.nextInt(distinctNumbers)).map(_.toLong).toList

      val eventualEstimatedDistinct = Source(elements)
        .countDistinct(k = 4096, getId = identity)
        .take(sampleSize)
        .runFold(List.empty[Double])((acc, item) => item :: acc)
        .map(_.head)

      val estimatedDinstict: Double = Await.result(eventualEstimatedDistinct, 1 second)
      estimatedDinstict must be between(distinctNumbers * 0.95, distinctNumbers * 1.05)
    }

    "count the distinct elements of a flow" in {

      val elements:List[Long] = Random.shuffle(for { i <- 0 to totalNumbers } yield Random.nextInt(distinctNumbers)).map(_.toLong).toList

      val countFlow: Flow[Long, Double, Unit] = Flow[Long].countDistinct(k = 4096, getId = identity)
      val eventualEstimatedDistinct = Source(elements)
        .via(countFlow)
        .take(sampleSize)
        .runFold(List.empty[Double])((acc, item) => item :: acc)
        .map(_.head)

      val estimatedDinstict: Double = Await.result(eventualEstimatedDistinct, 1 second)
      estimatedDinstict must be between(distinctNumbers * 0.95, distinctNumbers * 1.05)
    }

    "branch a flow based on a predicate" in {


      val source = Source((0 to 9).toList)

      val (left,right) = Flow[Int].branch(_ % 2 == 0)

      val eventualOdds = source
        .via(left)
        .runFold(List.empty[Int])((acc, item) => item :: acc)

      val eventualEven = source
        .via(right)
        .runFold(List.empty[Int])((acc, item) => item :: acc)


      val odds = Await.result(eventualOdds, 1 second)
      val even = Await.result(eventualEven, 1 second)

      odds must containAllOf(List(1,3,5,7,9))
      even must containAllOf(List(0,2,4,6,8))
    }
  }

}
