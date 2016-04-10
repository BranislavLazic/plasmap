package io.plasmap.util

import com.twitter.util.Base64StringEncoder._
import io.plasmap.model.{OsmRelation, OsmWay, OsmNode, OsmObject}
import io.plasmap.serializer.OsmSerializer._

import scala.util.Try

/**
 * Created by janschulte on 19/03/15.
 */
object GeowUtils {

  def isNode(elem: OsmObject): Boolean = elem match {
    case OsmNode(id,user,version,tags,point) => true
    case _ => false
  }

  def isWay(elem: OsmObject): Boolean = elem match {
    case OsmWay(id,user,version,tags,nds) => true
    case _ => false
  }

  def isRelation(elem: OsmObject): Boolean = elem match {
    case OsmRelation(id,user,version,tags,refs) => true
    case _ => false
  }

  def encodeBase64(element: OsmObject): String = encode(toBinary(element))

  def decodeBase64(serialized: String): Try[OsmObject] = fromBinary(decode(serialized))

}
