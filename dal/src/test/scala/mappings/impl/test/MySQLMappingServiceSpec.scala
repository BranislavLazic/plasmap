package io.plasmap.geo.mappings.impl.test

import io.plasmap.geo.mappings._
import io.plasmap.geo.mappings.impl.MySQLMappingService
import io.plasmap.model.OsmId
import org.joda.time.DateTime
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MySQLMappingServiceSpec extends Specification with ScalaCheck {

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

  val service = MySQLMappingService()

  "The OsmMappingService" should {

    "successfully insert an \"OsmNodeMapping\"" ! prop{ mapping: OsmNodeMapping =>
      {
        val result = Await.result(service.insertNodeMapping(mapping), duration)
        result must beSome(mapping)
        Thread.sleep(100)
        val future = service.findNodeMapping(mapping.osmId)
        val result2 = Await.result(future, duration)
        result2 must beSome(mapping)
      }
    }
    
    "successfully insert an \"OsmWayMapping\"" ! prop{ mapping: OsmWayMapping =>
      {
        val result = Await.result(service.insertWayMapping(mapping), duration)
        result must beSome(mapping)
        Thread.sleep(100)
        val future = service.findWayMapping(mapping.osmId)
        val result2 = Await.result(future, duration)
        result2 must beSome(mapping)
      }
    }
    
    "successfully insert an \"OsmRelationMapping\"" ! prop{ mapping: OsmRelationMapping =>
      {
        val result = Await.result(service.insertRelationMapping(mapping), duration)
        result must beSome(mapping)
        Thread.sleep(100)
        val future = service.findRelationMapping(mapping.osmId)
        val result2 = Await.result(future, duration)
        result2 must beSome(mapping)
      }
    }
  }

}