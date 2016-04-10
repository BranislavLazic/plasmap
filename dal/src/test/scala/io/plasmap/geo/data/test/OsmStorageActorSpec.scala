package io.plasmap.geo.data.test

import io.plasmap.geo.data.OsmStorageActor._
import io.plasmap.geo.data.typed.StorageEntriesBBTag
import io.plasmap.geo.data.{OsmBB, _}
import io.plasmap.util.test.AkkaTestkitSpecs2Support
import io.plasmap.generator.OsmObjectGenerator
import io.plasmap.model.{OsmId, OsmTypeNode, OsmTypeRelation, OsmTypeWay}
import org.scalacheck.Gen._
import org.scalacheck._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import akka.pattern.ask
import scala.None
import scala.collection.immutable.IndexedSeq
import scala.concurrent.{Await, Future}
import scala.util.Random

import scala.concurrent.duration._

/* Both Akka and Specs2 add implicit conversions for adding time-related
   methods to Int. Mix in the Specs2 NoTimeConversions trait to avoid a clash. */
class OsmStorageActorSpec extends Specification with NoTimeConversions with ScalaCheck {

  sequential

  val generator = OsmObjectGenerator()

  import generator._

  val random = new Random()

  val nodeBB = for {
    bb <- 0L
    osmId <- generateOsmId
    node <- generateDenormalizedNode
  } yield OsmBB(bb, osmId, node)

  val wayBB = for {
    bb <- 0L
    osmId <- generateOsmId
    way <- generateDenormalizedWay
  } yield OsmBB(bb, osmId, way)

  val relationBB = for {
    bb <- 0L
    osmId <- generateOsmId
    relation <- generateDenormalizedRelation
  } yield OsmBB(bb, osmId, relation)

  val nodeBBTag = for {
    bb <- 0L
    osmId <- generateOsmId
    tag <- generateTag
    node <- generateDenormalizedNode
  } yield OsmBBTag(bb, osmId, tag, node)

  val wayBBTag = for {
    bb <- 0L
    osmId <- generateOsmId
    tag <- generateTag
    way <- generateDenormalizedWay
  } yield OsmBBTag(bb, osmId, tag, way)


  val relationBBTag = for {
    bb <- 0L
    osmId <- generateOsmId
    tag <- generateTag
    relation <- generateDenormalizedRelation
  } yield OsmBBTag(bb, osmId, tag, relation)

  val service = OsmStorageService()

  Thread.sleep(5000)

  "An \"OsmStorageActor\"" should {

    "store and retrieve an \"OsmBB\" with an \"OsmDenormalizedNode\"" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      /* Storage actor needs some time for startup */
      Thread.sleep(1000)

      implicit def nodeBBArb = Arbitrary {
        nodeBB
      }

      prop { (elementBB: OsmBB) => {
        storageActor ! StoreBB(elementBB)

        Thread.sleep(20)
        storageActor ! GetBB(elementBB.bb, elementBB.osmId, OsmTypeNode)

        val result = expectMsgType[EntryBB](max = 1 second)
        result.elementBB must beSome(elementBB)
      }
      }

    }
    "store and retrieve an \"OsmBB\" with an \"OsmDenormalizedWay\"" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      /* Storage actor needs some time for startup */
      Thread.sleep(1000)

      implicit def wayBBArb = Arbitrary {
        wayBB
      }

      prop { (elementBB: OsmBB) => {
        storageActor ! StoreBB(elementBB)

        Thread.sleep(20)
        storageActor ! GetBB(elementBB.bb, elementBB.osmId, OsmTypeWay)

        val result = expectMsgType[EntryBB](max = 1 second)
        result.elementBB must beSome(elementBB)
      }
      }

    }
    "store and retrieve an \"OsmBB\" with an \"OsmDenormalizedRelation\"" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      /* Storage actor needs some time for startup */
      Thread.sleep(1000)

      implicit def relationBBArb = Arbitrary {
        relationBB
      }

      prop { (elementBB: OsmBB) => {
        storageActor ! StoreBB(elementBB)

        Thread.sleep(50)
        storageActor ! GetBB(elementBB.bb, elementBB.osmId, OsmTypeRelation)

        val result = expectMsgType[EntryBB](max = 1 second)
        result.elementBB must beSome(elementBB)
      }
      }

    }
    "store and retrieve an \"OsmBBTag\" with an \"OsmDenormalizedNode\"" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      /* Storage actor needs some time for startup */
      Thread.sleep(1000)

      implicit def nodeBBTagArb = Arbitrary {
        nodeBBTag
      }

      prop { (elementBBTag: OsmBBTag) => {
        storageActor ! StoreBBTag(elementBBTag)

        Thread.sleep(20)
        storageActor ! GetBBTag(elementBBTag.bb, elementBBTag.osmId, elementBBTag.tag, OsmTypeNode)

        val result = expectMsgType[EntryBBTag](max = 1 second)
        result.elementBBTag must beSome(elementBBTag)
      }
      }

    }
    "store and retrieve an \"OsmBBTag\" with an \"OsmDenormalizedWay\"" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      /* Storage actor needs some time for startup */
      Thread.sleep(1000)

      implicit def wayBBTagArb = Arbitrary {
        wayBBTag
      }

      prop { (elementBBTag: OsmBBTag) => {
        storageActor ! StoreBBTag(elementBBTag)

        Thread.sleep(100)
        storageActor ! GetBBTag(elementBBTag.bb, elementBBTag.osmId, elementBBTag.tag, OsmTypeWay)

        val result = expectMsgType[EntryBBTag](max = 1 second)
        result.elementBBTag must beSome(elementBBTag)
      }
      }

    }
    "store and retrieve an \"OsmBBTag\" with an \"OsmDenormalizedRelation\"" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      /* Storage actor needs some time for startup */
      Thread.sleep(1000)

      implicit def relationBBTagArb = Arbitrary {
        relationBBTag
      }

      prop { (elementBBTag: OsmBBTag) => {
        storageActor ! StoreBBTag(elementBBTag)

        Thread.sleep(500)
        storageActor ! GetBBTag(elementBBTag.bb, elementBBTag.osmId, elementBBTag.tag, OsmTypeRelation)

        val result = expectMsgType[EntryBBTag](max = 1 second)
        result.elementBBTag must beSome(elementBBTag)
      }
      }

    }
    "store and retrieve a list of denormalized nodes by bb and tag" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      val bb = random.nextLong()
      val tag = generateTag

      val elements = (for(i <- 1 to 100) yield OsmBBTag(bb,OsmId(i),tag,generateDenormalizedNode)).toList

      for(element <- elements) storageActor ! StoreBBTag(element)

      private val list: List[EntryBBTag] = elements.map(x => EntryBBTag(Some(x)))
      expectMsgAllOf(10 seconds, list :_*)

      storageActor ! GetElementsByBBandTag(bb, tag, OsmTypeNode)

      expectMsg(10 seconds, EntriesBBTag(elements))

    }
    "store and retrieve a list of denormalized ways by bb and tag" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      val bb = random.nextLong()
      val tag = generateTag

      val elements = (for(i <- 1 to 100) yield OsmBBTag(bb,OsmId(i),tag,generateDenormalizedWay)).toList

      for(element <- elements) storageActor ! StoreBBTag(element)

      private val list: List[EntryBBTag] = elements.map(x => EntryBBTag(Some(x)))
      expectMsgAllOf(10 seconds, list :_*)

      storageActor ! GetElementsByBBandTag(bb, tag, OsmTypeWay)

      expectMsg(10 seconds, EntriesBBTag(elements))

    }
    "store and retrieve a list of denormalized relations by bb and tag" in new AkkaTestkitSpecs2Support {

      val storageActor = system.actorOf(OsmStorageActor.props(),"storage")

      val bb = random.nextLong()
      val tag = generateTag

      val elements = (for(i <- 1 to 10) yield OsmBBTag(bb,OsmId(i),tag,generateDenormalizedRelation)).toList

      for(element <- elements) storageActor ! StoreBBTag(element)

      private val list: List[EntryBBTag] = elements.map(x => EntryBBTag(Some(x)))
      expectMsgAllOf(10 seconds, list :_*)

      storageActor ! GetElementsByBBandTag(bb, tag, OsmTypeRelation)

      expectMsg(10 seconds, EntriesBBTag(elements))

    }
  }

}
