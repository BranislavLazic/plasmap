package io.plasmap.geo.mappings.test

import akka.util.ByteString
import io.plasmap.geo.mappings._
import io.plasmap.model.OsmId
import org.joda.time.DateTime
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class MappingSerializerSpec extends Specification with ScalaCheck {

  sequential

  val posLong = Arbitrary.arbitrary[Long] suchThat (_ >= 0L)

  val nodeMapping = for {
    bb <- arbitrary[Long]
    osmId <- posLong
    updated = DateTime.now
  } yield OsmNodeMapping(bb, OsmId(osmId), updated)

  implicit def nodeMappingArb = Arbitrary {
    nodeMapping
  }

  val wayMapping = for {
    bb <- arbitrary[Long]
    osmId <- posLong
    updated = DateTime.now
  } yield OsmWayMapping(bb, OsmId(osmId), updated)

  implicit def wayMappingArb = Arbitrary {
    wayMapping
  }

  val relationMapping = for {
    osmId <- posLong
    bb <- arbitrary[Long]
    updated = DateTime.now
  } yield OsmRelationMapping(bb, OsmId(osmId), updated)

  implicit def relationMappingArb = Arbitrary {
    relationMapping
  }


  import MappingSerializer._

  "The MappingSerializer" should {

    "successfully insert an \"OsmNodeMapping\"" ! prop { mapping: OsmNodeMapping => {
      val serialized: ByteString = nodeMappingSerializer.serialize(mapping)
      val deserialized = nodeMappingSerializer.deserialize(serialized)
      deserialized must be_==(mapping)
    }
    }

    "successfully insert an \"OsmWayMapping\"" ! prop { mapping: OsmWayMapping => {
      val serialized: ByteString = wayMappingSerializer.serialize(mapping)
      val deserialized = wayMappingSerializer.deserialize(serialized)
      deserialized must be_==(mapping)
    }
    }

    "successfully insert an \"OsmRelationMapping\"" ! prop { mapping: OsmRelationMapping => {
      val serialized: ByteString = relationMappingSerializer.serialize(mapping)
      val deserialized = relationMappingSerializer.deserialize(serialized)
      deserialized must be_==(mapping)
    }
    }
  }

}