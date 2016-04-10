import akka.util.ByteString
import io.plasmap.geo.mappings.MappingSerializer
import io.plasmap.geohash.{PrecisionLow_20KM, GeoHash}

val s = MappingSerializer.nodeMappingSerializer
println(s)
val mapping = s.deserialize(ByteString("""\x00\xd0a\xb1\x9b@Z\x89t\x00\x00\x00\x00\xd7\xa2\xd6Y\x00\x00\x01Q`\x06\x82\xbe"""))
println(mapping)
val bb: Long = GeoHash.ultraHigh.reduceParallelPrecision(mapping.hash,PrecisionLow_20KM)
println(bb