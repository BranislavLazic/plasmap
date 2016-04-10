import akka.util.ByteString
import io.plasmap.geo.mappings.{OsmWayMapping, MappingSerializer, OsmNodeMapping}
import io.plasmap.model.OsmId
import org.joda.time.DateTime

val hash: Long = 31940983851L
val id: OsmId = OsmId(3342534L)
val now: DateTime = DateTime.now()

val mapping = OsmWayMapping(hash,id,now)
val x: ByteString = MappingSerializer.wayMappingSerializer.serialize(mapping)

val act = MappingSerializer.wayMappingSerializer.deserialize(x)
act.get == mapping

