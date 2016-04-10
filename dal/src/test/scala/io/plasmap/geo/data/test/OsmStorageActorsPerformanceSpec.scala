package io.plasmap.geo.data.test

import akka.util.Timeout
import io.plasmap.geo.data.OsmStorageActor._
import io.plasmap.geo.data.{OsmBB, OsmStorageActor}
import io.plasmap.util.test.AkkaTestkitSpecs2Support
import io.plasmap.model.OsmTypeNode
import io.plasmap.generator.OsmObjectGenerator
import org.specs2.ScalaCheck
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.time.NoTimeConversions
import akka.routing.RoundRobinPool
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}
import scala.util.Random
import akka.pattern.ask

/* Both Akka and Specs2 add implicit conversions for adding time-related
   methods to Int. Mix in the Specs2 NoTimeConversions trait to avoid a clash. */
class OsmStorageActorsPerformanceSpec extends Specification with NoTimeConversions /*with ScalaCheck*/ {

  val generator = OsmObjectGenerator()
  
  import generator._

  "An \"OsmStorageActor\"" should {

    "10k writes under 60s" in new AkkaTestkitSpecs2Support {
      val writer = system.actorOf(new RoundRobinPool(50).props(OsmStorageActor.props()))
      val reader = system.actorOf(OsmStorageActor.props())

      def read(bb: Long): Long = {
        val duration = Duration(10, SECONDS)
        Await.result(reader.ask(CountBB(bb, OsmTypeNode))(Timeout(duration)).mapTo[Long], duration)
      }

      def write(bb: Long, count: Int, seconds: Int): Result = {

        (1 to count).foreach(id => {
          val nodeBB = OsmBB(bb, generateOsmId, generateDenormalizedNode)
          writer ! StoreBB(nodeBB)
        })
        Thread.sleep(seconds * 1000)
        read(bb) mustEqual count
      }

      val bb = Random.nextLong
      write(bb, count = 10000, seconds = 60)
    }


  }

}
