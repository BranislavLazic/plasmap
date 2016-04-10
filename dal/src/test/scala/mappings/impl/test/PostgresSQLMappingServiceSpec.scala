package io.plasmap.geo.mappings.impl.test

import io.plasmap.geo.mappings._
import io.plasmap.geo.mappings.impl.PostgresSQLMappingService
import io.plasmap.model.OsmId
import org.joda.time.DateTime
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class PostgresSQLMappingServiceSpec extends Specification with ScalaCheck {

  sequential

  val duration = 30 seconds

   val posLong = Arbitrary.arbitrary[Long] suchThat (_ >= 0L)
   
  val nodeMapping = for {
    bb <- arbitrary[Long]
    osmId <- posLong
    updated = DateTime.now
  } yield OsmNodeMapping(bb, OsmId(osmId), updated)

  implicit def nodeMappingArb = Arbitrary { nodeMapping }
  
  val wayMapping = for {
    bb <- arbitrary[Long]
    osmId <- posLong
    updated = DateTime.now
  } yield OsmWayMapping(bb, OsmId(osmId), updated)

  implicit def wayMappingArb = Arbitrary { wayMapping }
  
  val relationMapping = for {
    osmId <- posLong
    bb <- arbitrary[Long]
    updated = DateTime.now
  } yield OsmRelationMapping(bb, OsmId(osmId), updated)

  implicit def relationMappingArb = Arbitrary { relationMapping }

  val service = PostgresSQLMappingService()

  "The OsmMappingService" should {

    "successfully insert an \"OsmNodeMapping\"" ! prop{ mapping: OsmNodeMapping =>
      {
        val result1 = Await.result(service.insertNodeMapping(mapping), duration)
        result1 must beSome(mapping)
        Thread.sleep(100)

        val result2 = Await.result(service.findNodeMapping(mapping.osmId), duration)
        result2 must beSome(mapping)

        val result3 = Await.result(service.deleteNodeMapping(mapping), duration)
        result3 must beSome(mapping)

        val result4 = Await.result(service.findNodeMapping(mapping.osmId), duration)
        result4 must beNone
      }
    }
    
    "successfully insert an \"OsmWayMapping\"" ! prop{ mapping: OsmWayMapping =>
      {
        val result1 = Await.result(service.insertWayMapping(mapping), duration)
        result1 must beSome(mapping)
        Thread.sleep(100)

        val result2 = Await.result(service.findWayMapping(mapping.osmId), duration)
        result2 must beSome(mapping)

        val result3 = Await.result(service.deleteWayMapping(mapping), duration)
        result3 must beSome(mapping)

        val result4 = Await.result(service.findWayMapping(mapping.osmId), duration)
        result4 must beNone
      }
    }
    
    "successfully insert an \"OsmRelationMapping\"" ! prop{ mapping: OsmRelationMapping =>
      {
        val result1 = Await.result(service.insertRelationMapping(mapping), duration)
        result1 must beSome(mapping)
        Thread.sleep(100)

        val result2 = Await.result(service.findWayMapping(mapping.osmId), duration)
        result2 must beSome(mapping)

        val result3 = Await.result(service.deleteRelationMapping(mapping), duration)
        result3 must beSome(mapping)

        val result4 = Await.result(service.findRelationMapping(mapping.osmId), duration)
        result4 must beNone
      }
    }
  }

}