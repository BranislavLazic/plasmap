package io.plasmap.geo.mappings

import akka.util.ByteString
import io.plasmap.model.OsmId
import org.joda.time.DateTime
import redis.{ByteStringDeserializer, ByteStringSerializer}
import scodec.Attempt.{Failure, Successful}
import scodec.{DecodeResult, Attempt}
import scodec.bits.BitVector
import scodec.codecs._


/**
 * Created by janschulte on 22/10/15.
 */
object MappingSerializer {

  private[this] val mappingCodec = uint8 ~ int64 ~ int64 ~ int64
  type CODEC = (((Int, Long), Long), Long)

  private[this] val nodeIdentifier: Int = 0
  private[this] val wayIdentifier: Int = 1
  private[this] val relationIdentifier: Int = 2

  private[this] def encode(hash: Long, id: Long, ts: Long, identifier: Int): ByteString = {
    val encoded: Array[Byte] = mappingCodec.encode(identifier ~ hash ~ id ~ ts).toOption.get.toByteArray
    ByteString(encoded)
  }

  private[this] def decode[M <: OsmMapping](bs: ByteString, f: (CODEC) => M): Option[M] = {
    val bits: BitVector = BitVector(bs.asByteBuffer)
    mappingCodec.decode(bits) match {
      case Successful(decoded) =>
        Some(decoded.map(f).value)
      case Failure(err) => None
    }
  }

  val nodeMappingSerializer = new ByteStringSerializer[OsmNodeMapping]
    with ByteStringDeserializer[OsmNodeMapping] {

    override def serialize(data: OsmNodeMapping): ByteString = {
      val hash = data.hash
      val id = data.osmId.value
      val ts = data.updated.getMillis

      val identifier: Int = nodeIdentifier
      encode(hash, id, ts, identifier)
    }

    override def deserialize(bs: ByteString): OsmNodeMapping = {

      val f: (CODEC) => OsmNodeMapping = {
        case typ ~ hash ~ id ~ ts => OsmNodeMapping(hash, OsmId(id), new DateTime(ts))
      }

      decode(bs, f).get
    }
  }

  val wayMappingSerializer = new ByteStringSerializer[OsmWayMapping]
    with ByteStringDeserializer[OsmWayMapping] {

    override def serialize(data: OsmWayMapping): ByteString = {
      val hash = data.hash
      val id = data.osmId.value
      val ts = data.updated.getMillis

      val identifier: Int = wayIdentifier
      encode(hash, id, ts, identifier)
    }

    override def deserialize(bs: ByteString): OsmWayMapping = {

      val f: (CODEC) => OsmWayMapping = {
        case typ ~ hash ~ id ~ ts => OsmWayMapping(hash, OsmId(id), new DateTime(ts))
      }

      decode(bs, f).get
    }
  }

  val relationMappingSerializer = new ByteStringSerializer[OsmRelationMapping]
    with ByteStringDeserializer[OsmRelationMapping] {

    override def serialize(data: OsmRelationMapping): ByteString = {
      val hash = data.hash
      val id = data.osmId.value
      val ts = data.updated.getMillis

      val identifier: Int = relationIdentifier
      encode(hash, id, ts, identifier)
    }

    override def deserialize(bs: ByteString): OsmRelationMapping = {

      val f: (CODEC) => OsmRelationMapping = {
        case typ ~ hash ~ id ~ ts => OsmRelationMapping(hash, OsmId(id), new DateTime(ts))
      }

      decode(bs, f).get
    }
  }

}
