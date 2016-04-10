package io.plasmap.util

import com.vividsolutions.jts.geom.{Coordinate => JTSCoordinate, CoordinateSequence => JTSCoordinateSequence, LineString => JTSLineString, LinearRing => JTSLinearRing, MultiPolygon => JTSMultiPolygon, Point => JTSPoint, Polygon => JTSPolygon}
import org.geotools.geometry.jts.JTSFactoryFinder
import io.plasmap.model.geometry._

import scala.annotation.tailrec
import scala.util.Try

/**
 * Created by mark on 19.05.15.
 * Converts the Geow Geometry objects to JTSGeometry objects such that they can be used for
 * geometric computations with the JTS library
 */
object GeowGeometryToJTSGeometry {
  val factory = JTSFactoryFinder.getGeometryFactory
  def Coordinate(x:Double, y:Double):JTSCoordinate = new JTSCoordinate(x,y)
  val DefaultCoordinate:JTSCoordinate = Coordinate(0,0)

  implicit def doubleTuple2JTSCoordinate(tup:(Double, Double)) :JTSCoordinate = Coordinate(tup._1, tup._2)
  implicit def jtsCoordinate2DoubleTuple(jtsC:JTSCoordinate):(Double, Double) = (jtsC.x, jtsC.y)

  implicit def doubleTupleList2JTSCoordinateArray(l:List[(Double, Double)]):Array[JTSCoordinate] =
    l.map(doubleTuple2JTSCoordinate).toArray

//  implicit def jtsCoordinateArray2DoubleTupleList(a:Array[JTSCoordinate]):List[(Double, Double)] =
//    a.map(jtsCoordinate2DoubleTuple).toList

  implicit def geowPoint2JTSPoint(gp:Point):JTSPoint = factory.createPoint(Coordinate(gp.lon, gp.lat))

  implicit def jtsPoint2GeowPoint(jtsp:JTSPoint):Point = {
    val coord:JTSCoordinate = Try(jtsp.getCoordinates()(0)).toOption.getOrElse(DefaultCoordinate)
    Point(coord.x, coord.y)
  }

  implicit def geowLineString2JTSLineString(ls:LineString):JTSLineString =
    factory.createLineString(ls.coordinates)

//  implicit def jtsLineString2GeowLineString(jtsLineString: JTSLineString):LineString =
//    LineString(jtsLineString.getCoordinates)
  
  @tailrec
  def fillUpDoubleListToLinearRing(l:List[(Double, Double)]):List[(Double, Double)] = {
    if(l.length < 3)
      fillUpDoubleListToLinearRing(l.head.copy(
        _1 = l.head._1 + 0.00001,
        _2 = l.head._2 + 0.00001) :: l
      )
    else
      l.last :: l
  }

  def doubleTupleList2JTSLinearRingOption(ls:List[(Double, Double)]):Option[JTSLinearRing] =
    if(GeometryUtil.isLinearRing(ls))
      Some(factory.createLinearRing(ls))
    else None

  implicit def geowLineString2JTSLinearRing(ls:List[(Double, Double)]):JTSLinearRing =
    doubleTupleList2JTSLinearRingOption(ls).getOrElse{
      factory.createLinearRing(fillUpDoubleListToLinearRing(ls))
  }

  implicit def doubleTupleListList2JTSLinearRingArray(a:List[List[(Double, Double)]]):Array[JTSLinearRing] = {
    a.flatMap(doubleTupleList2JTSLinearRingOption).toArray
  }

  implicit def doubleTupleListList2JTSPolygon(l:List[List[(Double, Double)]]): JTSPolygon = {
    factory.createPolygon(l.head, l.tail)
  }

  implicit def geowPolygon2JTSPolygon(poly:Polygon):JTSPolygon = {
    doubleTupleListList2JTSPolygon(poly.coordinates)
  }

  implicit def geowMultiPolygon2JTSMultiPolygon(mp:MultiPolygon):JTSMultiPolygon = {
    def f = Polygon.apply _ andThen geowPolygon2JTSPolygon
    factory.createMultiPolygon(mp.coordinates.map(f).toArray)
  }
}
