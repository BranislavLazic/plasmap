package io.plasmap.geo.mappings

import io.plasmap.model.OsmId
import org.joda.time.DateTime

sealed trait OsmMapping {

  def hash: Long
  def osmId: OsmId
  def updated: DateTime

  override def hashCode = 41 * (41 + osmId.hashCode) + hash.hashCode //Magic
  override def equals(other: Any) = other match {
    case that: OsmMapping =>
      (that canEqual this) &&
        (this.osmId == that.osmId) && (this.hash == that.hash)

    case _ => false
  }
  def canEqual(other: Any) = other.isInstanceOf[OsmMapping]
  
}
case class OsmNodeMapping(hash: Long, osmId: OsmId, updated: DateTime) extends OsmMapping
case class OsmWayMapping(hash: Long, osmId: OsmId, updated: DateTime) extends OsmMapping
case class OsmRelationMapping(hash: Long, osmId: OsmId, updated: DateTime) extends OsmMapping