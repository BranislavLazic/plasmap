package io.plasmap.geo.util

import io.plasmap.model.OsmObject
import kafka.serializer.Encoder

object OsmObjectEncoder {

  object OsmObjectEncoder extends Encoder[OsmObject] {

    import io.plasmap.serializer.OsmSerializer._

    override def toBytes(t: OsmObject): Array[Byte] = {
      toBinary(t)
    }

    def partitionizer(osmObject: OsmObject) =
      Some(osmObject.id.value.toString.getBytes("UTF8"))
  }


}
