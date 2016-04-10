package io.plasmap.geo.mappings.impl.test

import java.util.concurrent.{Executors, TimeoutException}

import io.plasmap.geo.mappings._
import io.plasmap.geo.mappings.impl.MySQLMappingService
import io.plasmap.model.OsmId
import org.joda.time.DateTime
import org.scalameter.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

object MySQLMappingServicePerformanceTest
  extends PerformanceTest.Quickbenchmark {


  /* inputs */

  val r = new Random()

  val sizes: Gen[Int] = Gen.range("size")(100, 200, 100)

  val mappings: Gen[List[OsmNodeMapping]] = for {
    size <- sizes
  } yield (0 to size).map(i => OsmNodeMapping(r.nextLong(), OsmId(r.nextLong()), DateTime.now)).toList

  val parallelismLevels = Gen.enumeration("parallelismLevel")(1, 2)
  val pools = (for (par <- parallelismLevels) yield executionContext(par)).cached

  def executionContext(par: Int): ExecutionContext = new ExecutionContext {

    val threadPool = Executors.newFixedThreadPool(par)

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }


  val inputs = Gen.tupled(mappings, pools)

  val service = MySQLMappingService()


  /* tests */

  performance of "OsmMappingService" in {
    measure method "insertMapping" in {
      using(inputs) config (
        exec.maxWarmupRuns -> 5
        ) in { case (mappings, ec) =>

        implicit val ex = ec
        val futures: List[Future[Option[OsmMapping]]] = mappings map { mapping =>
          service.insertMapping(mapping) recover { case _:TimeoutException => None }
        }

        Await.result(Future.sequence(futures), 300 seconds)
      }
    }
  }

}