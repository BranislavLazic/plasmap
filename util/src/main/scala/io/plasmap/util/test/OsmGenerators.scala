package io.plasmap.util.test

import io.plasmap.model._
import io.plasmap.model.geometry._
import io.plasmap.util.GeowGeometryToJTSGeometry._
import org.joda.time.DateTime
import org.scalacheck.Gen


/**
 * Created by mark on 11.02.15.
 */
object OsmGenerators {

  def optional[A](a:Gen[A]) = Gen.oneOf(a.map(Some.apply), Gen.const(None:Option[A]))

  val osmVersionG = {
    val earlyDate = (new DateTime).withYear(2000).withMonthOfYear(1).withDayOfMonth(1)
    for {
      timestamp ← Gen.choose(earlyDate.getMillis, DateTime.now.getMillis)
      version   ← Gen.choose(1, 30000)
      changeSet ← Gen.choose(1, Int.MaxValue)
    } yield OsmVersion(timestamp, version, changeSet, visible = true)
  }

  val osmIdG = for (osmId ← Gen.choose(0L, Long.MaxValue)) yield OsmId(osmId)

  val osmUserG = for {
    userName ← Gen.alphaStr //TODO: Consider Unicode Strings
    userId   ← Gen.choose(0L, Long.MaxValue)
  } yield OsmUser(userName, userId)

  val osmTagG = for {
    key   ← Gen.oneOf(OsmTagKeys.allKeys)
    value ← Gen.alphaStr //TODO: Consider Unicode Strings
  } yield OsmTag(key, value)

  val osmTagsG = Gen.listOf(osmTagG)

  val osmPointG = for {
    lon ← Gen.choose(-90.0, 90.0)
    lat ← Gen.choose(-180.0, 180.0) suchThat (_ > -180.0)
  } yield Point(lon, lat)

  val osmDoubleTupleG: Gen[(Double, Double)] = osmPointG.map(llp ⇒ (llp.lon, llp.lat))

  val osmRoleG = for {
    other ← Gen.alphaStr
    role ← Gen.oneOf(OsmRoleInner, OsmRoleOuter, OsmRoleEmpty, OsmRoleOther(other))
  } yield role

  val osmMemberG = for {
    typ  ← Gen.oneOf(OsmTypeNode, OsmTypeWay, OsmTypeRelation)
    ref  ← osmIdG
    role ← osmRoleG
  } yield OsmMember(typ, ref, role)

  val osmNodeG = for {
    id <- osmIdG
    user <- osmUserG
    version <- osmVersionG
    tags       ← osmTagsG
    point      ← osmPointG
  } yield OsmNode(id,Some(user),version, tags, point)
  
  val osmWayG = for {
    id <- osmIdG
    user <- osmUserG
    version <- osmVersionG
    tags       ← osmTagsG
    nodes      ← Gen.listOf(osmIdG)
  } yield OsmWay(id,Some(user),version, tags, nodes)

  val osmRelationG = for {
    id <- osmIdG
    user <- osmUserG
    version <- osmVersionG
    tags       ← osmTagsG
    members    ← Gen.listOf(osmMemberG)
  } yield OsmRelation(id,Some(user),version, tags, members)

  val osmDenormalizedNodeG = for {
    id <- osmIdG
    user <- osmUserG
    version <- osmVersionG
    tags       ← osmTagsG
    point      ← osmPointG
  } yield OsmDenormalizedNode(id,Some(user),version, tags, point)

  val lineStringG: Gen[LineString] = for {
    points1     ← Gen.nonEmptyListOf(osmDoubleTupleG)
    points2     ← Gen.nonEmptyListOf(osmDoubleTupleG)
    linestring   =  LineString(points1 ++ points2)
  } yield linestring

  val linearRingG: Gen[LineString] = for {
    p1   ← osmDoubleTupleG
    ps   ← Gen.nonEmptyListOf(osmDoubleTupleG)
    last ← osmDoubleTupleG
    linearRing = LineString(p1 :: ps ++ (last :: p1 :: Nil))
  } yield linearRing

  val polygonG: Gen[Polygon] = for {
    ring1:LineString ← linearRingG
    ring2:LineString ← linearRingG if ring2.within(ring1)
  } yield Polygon(List(ring1.coordinates, ring2.coordinates))

  val multiPolygonG: Gen[MultiPolygon] = for {
    poly1:Polygon ← polygonG
    poly2:Polygon ← polygonG
  } yield MultiPolygon(List(poly1.coordinates, poly2.coordinates))

  val osmDenormalizedWayG = for {
    id <- osmIdG
    user <- osmUserG
    version <- osmVersionG
    tags       ← osmTagsG
    linestring ← lineStringG
  } yield OsmDenormalizedWay(id,Some(user),version, tags, linestring)

//  val osmGeometryMemberNodeG: Gen[GeometryMember] = for {
//    id ← osmIdG
//    role ← osmRoleG
//    point ← osmPointG
//  } yield GeometryMember(OsmTypeNode, id, role, point)
//
//  val osmGeometryMemberWayG: Gen[GeometryMember] = for {
//    id ← osmIdG
//    role ← osmRoleG
//    points     ← Gen.listOf(osmPointG)
//    linestring = Linestring(points)
//  } yield GeometryMember(OsmTypeWay, id, role, linestring)
//
//  val osmGeometryRelationG = for {
//    members:List[GeometryMember] ← Gen.lzy(Gen.nonEmptyListOf(osmGeometryMemberG))
//  } yield GeometryCollection(members)

  val geometryCollectionG = for {
    geo1 ← Gen.oneOf(osmPointG, multiPolygonG, multiPolygonG)
    geo2 ← Gen.oneOf(osmPointG, osmPointG, osmPointG, lineStringG)
  } yield GeometryCollection(List(geo1, geo2))
//
//  val osmGeometryMemberRelationG:Gen[GeometryMember] = for {
//    id ← osmIdG
//    role ← osmRoleG
//    member ← Gen.lzy(Gen.nonEmptyListOf(osmGeometryRelationG))
//    geometry ← osmGeometryRelationG
//  } yield GeometryMember(OsmTypeRelation, id, role, geometry)

//  val osmGeometryMemberG:Gen[GeometryMember] = Gen.oneOf(osmGeometryMemberNodeG, osmGeometryMemberWayG, osmGeometryMemberRelationG)

  val osmDenormalizedRelationG = for {
    id <- osmIdG
    user <- osmUserG
    version <- osmVersionG
    tags       ← osmTagsG
    geometry   ← geometryCollectionG
  } yield OsmDenormalizedRelation(id,Some(user),version, tags, geometry)
}

object OsmTagKeys {
  lazy val allKeys:List[String] = {
    val source = scala.io.Source.fromFile("util/src/test/resources/tags.txt", "utf-8")
    val result = source.getLines().toList
    source.close()
    result
  }
}