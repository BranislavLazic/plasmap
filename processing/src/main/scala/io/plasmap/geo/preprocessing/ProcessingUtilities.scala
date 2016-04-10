package io.plasmap.geo.preprocessing

import io.plasmap.geo.data.{OsmBBTag, OsmBB}
import io.plasmap.geo.mappings._
import io.plasmap.model._
import io.plasmap.util.OsmObjectMapper
import org.joda.time.DateTime

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
object ProcessingUtilities {

  def toMapping(osmObject: OsmDenormalizedObject): OsmMapping = {
    val osmId = osmObject.id
    val dt = DateTime.now()

    osmObject match {
      case node: OsmDenormalizedNode ⇒
        //TODO: Think about a proper mapping for points
        OsmNodeMapping(node.geometry.hash, osmId, dt)
      case way: OsmDenormalizedWay ⇒ OsmWayMapping(OsmObjectMapper.principalBoundingBox(osmObject), osmId, dt)
      case relation: OsmDenormalizedRelation ⇒ OsmRelationMapping(OsmObjectMapper.principalBoundingBox(osmObject), osmId, dt)
    }
  }

  def toBB(osmObject:OsmDenormalizedObject):OsmBB = {
    val bb = OsmObjectMapper.principalBoundingBox(osmObject)
    val osmId = osmObject.id
    OsmBB(bb, osmId, osmObject)
  }

  def toBBTag(osmObject:OsmDenormalizedObject): List[OsmBBTag] = {

    val osmId = osmObject.id
    val bbs = OsmObjectMapper.boundingBoxes(osmObject)
    val tags = OsmObjectMapper.tags(osmObject)

    val set: Set[OsmBBTag] = for {bb <- bbs; tag <- tags} yield {
      OsmBBTag(bb, osmId, tag, osmObject)
    }
    set.toList
  }


  /** The indexer will consider but these keys. */
  private[this] val allowedKeys = Set(
    //Names
    "name", "alt_name", "nat_name", "loc_name", "reg_name", "short_name",
    //Names in different languages
    "name:en", "name:de", "name:fr", "name:es", "name:ru",
    //Address tags
    "addr:housenumber", "addr:housename", "addr:street", "addr:place", "addr:postcode", "addr:flats", "addr:city",
    "addr:country", "addr:full"
  )

  private[this] val filterTags = (allowedKeys: Set[String]) ⇒ (tagsMap: Map[String, String]) ⇒
    tagsMap filterKeys (allowedKeys contains)
  private[this] val filterFunction: (Map[String, String]) => Map[String, String] = filterTags(allowedKeys)


  def toIndex(osmObject: OsmDenormalizedObject):Option[IndexMapping] = {

    val tags = osmObject.tags.map { case OsmTag(k, v) ⇒ (k, v) }.toMap
    val filtered: Map[String, String] = filterFunction(tags)
    if(filtered.nonEmpty) {
      // TODO: Switch to idris and use dependent type here :-)
      Some(IndexMapping(osmObject, filtered))
    }else{
      None
    }
  }
}
