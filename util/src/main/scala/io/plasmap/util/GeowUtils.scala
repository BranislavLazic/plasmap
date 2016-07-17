package io.plasmap.util

import com.twitter.util.Base64StringEncoder._
import io.plasmap.model._
import io.plasmap.serializer.OsmSerializer._

import scala.util.Try

object GeowUtils {


  def isNode(elem: OsmObject): Boolean = elem match {
    case _: OsmNode => true
    case _ => false
  }

  def isDenormalisedNode(elem: OsmDenormalizedObject): Boolean = elem match {
    case _: OsmDenormalizedNode => true
    case _ => false
  }

  def isWay(elem: OsmObject): Boolean = elem match {
    case _: OsmWay => true
    case _ => false
  }

  def isDenormalisedWay(elem: OsmDenormalizedObject): Boolean = elem match {
    case _: OsmDenormalizedWay => true
    case _ => false
  }

  def isRelation(elem: OsmObject): Boolean = elem match {
    case _: OsmRelation => true
    case _ => false
  }

  def isDenormalisedRelation(elem: OsmDenormalizedObject): Boolean = elem match {
    case _: OsmDenormalizedRelation => true
    case _ => false
  }

  def toOsmType(elem: OsmObject): OsmType = elem match {
    case elem: OsmNode => OsmTypeNode
    case elem: OsmWay => OsmTypeWay
    case elem: OsmRelation => OsmTypeRelation
  }

  def toOsmType(elem: OsmDenormalizedObject): OsmType = elem match {
    case elem: OsmDenormalizedNode => OsmTypeNode
    case elem: OsmDenormalizedWay => OsmTypeWay
    case elem: OsmDenormalizedRelation => OsmTypeRelation
  }

  def encodeBase64(element: OsmObject): String = encode(toBinary(element))

  def decodeBase64(serialized: String): Try[OsmObject] = fromBinary(decode(serialized))

}
