package io.plasmap.geo.util

import io.plasmap.model.OsmDenormalizedObject
import kafka.serializer.Encoder

object OsmDenormalisedObjectEncoder extends Encoder[OsmDenormalizedObject] {

  import io.plasmap.serializer.OsmDenormalizedSerializer._

  override def toBytes(t: OsmDenormalizedObject): Array[Byte] = {
    toBinary(t)
  }

  def partitionizer(osmDenormalizedObject: OsmDenormalizedObject) =
    Some(osmDenormalizedObject.id.value.toString.getBytes("UTF8"))
}
