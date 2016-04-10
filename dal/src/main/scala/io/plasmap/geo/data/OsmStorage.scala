package io.plasmap.geo.data

import io.plasmap.model.{OsmDenormalizedObject, OsmId, OsmTag}


sealed trait OsmStorage {
  
  val bb: Long
  val osmId: OsmId
  val element: OsmDenormalizedObject

}
case class OsmBB(bb: Long, osmId: OsmId, element : OsmDenormalizedObject) extends OsmStorage
case class OsmBBTag(bb: Long, osmId: OsmId, tag: OsmTag, element : OsmDenormalizedObject) extends OsmStorage
