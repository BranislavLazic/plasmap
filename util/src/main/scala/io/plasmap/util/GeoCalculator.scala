package io.plasmap.util

import com.vividsolutions.jts.geom
import com.vividsolutions.jts.geom.{Coordinate => JTSCoordinate, CoordinateSequence => JTSCoordinateSequence, LinearRing => JTSLinearRing, Point => JTSPoint, Polygon => JTSPolygon, GeometryFactory, Envelope}
import org.geotools.geometry.jts.JTSFactoryFinder
import io.plasmap.geohash.GeoHash
import io.plasmap.model.geometry._
import io.plasmap.model.{OsmDenormalizedNode, OsmDenormalizedObject, OsmDenormalizedRelation, OsmTypeWay}
import sun.security.util.PolicyUtil

import scala.collection.mutable.ArrayBuffer
import GeowGeometryToJTSGeometry._


/**
 * Utility class to perform geometric calculations on the geow osm model.
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
object GeoCalculator {

  private val geometryFactory = JTSFactoryFinder.getGeometryFactory(null)
  private val hashCreator = GeoHash.ultraHigh

  def multiPolysFromGeoColl(gc: GeometryCollection): List[MultiPolygon] = {
    gc.geometries.collect { case mp: MultiPolygon ⇒ mp } //TODO: Might have to flatten first.
  }

  // FIXME: Refactor: within method should be for the geometry. There is no need for having the osm object here.
  def within(inner:OsmDenormalizedObject,outer:OsmDenormalizedObject):Boolean = {
    (inner,outer) match {
      case (inner:OsmDenormalizedRelation,outer:OsmDenormalizedRelation) =>
        val innerMps: List[MultiPolygon] = multiPolysFromGeoColl(inner.geometry)
        val outerMps: List[MultiPolygon] = multiPolysFromGeoColl(outer.geometry)
        val isWithin = innerMps.forall(p => outerMps.exists(q => p.within(q)))
        if(isWithin) true else{
          val isAtLeast90PercentIn = innerMps.forall(p => outerMps.exists(q => (q.intersection(p).getArea / p.getArea) >= 0.90 ))
          isAtLeast90PercentIn
        }

      case (OsmDenormalizedNode(_,_,_,_,point),OsmDenormalizedRelation(_,_,_,_,gc)) =>

        val mps = multiPolysFromGeoColl(gc)
        val jtsPoint = geometryFactory.createPoint(new JTSCoordinate(point.lon,point.lat))
        mps.exists(_.contains(jtsPoint))
      case _ =>
        //TODO implement
        false
    }
  }

  // FIXME: Should work on geometry, not on relation
  def rectangle(relation: OsmDenormalizedRelation): (Point, Point) = {

    val coordsOpt = multiPolysFromGeoColl(relation.geometry).headOption.flatMap(_.coordinates.headOption)
    val maybePolygon: Option[JTSPolygon] = coordsOpt.map((x: List[List[(Double, Double)]]) ⇒ doubleTupleListList2JTSPolygon(x))
    maybePolygon match {
      case Some(polygon) =>
        val envelope: Envelope = polygon.getEnvelopeInternal

        val upperLeft = Point(envelope.getMinX, envelope.getMaxY)
        val lowerRight = Point(envelope.getMaxX, envelope.getMinY)
        (upperLeft, lowerRight)

      case None =>
        val points: List[Point] = (for {
          geometry <- relation.geometry.geometries
        } yield geometry match {
            case MultiPolygon(coordinates) =>
              coordinates.flatten.flatten.map(point => Point(point._1,point._2))
            case p:Point =>
              List(p)
            case LineString(coordinates) =>
              coordinates.map(point => Point(point._1,point._2))
            case _ =>
              List.empty[Point]
        }).flatten

        val lonSorted = points.sortBy(_.lon)
        val latSorted = points.sortBy(_.lat)

        (lonSorted,latSorted) match {
          case (minLon :: lonTail, minLat :: latTail) => (lonSorted.reverse,latSorted.reverse) match {
            case (maxLon :: lonTail2, maxLat :: latTail2) =>
              Point(minLon.lon,maxLat.lat) -> Point(maxLon.lon,minLat.lat)

            case _ => Point(6.7406842,51.2679772) -> Point(6.8250859, 51.2002376)
          }

          case _ => Point(6.7406842,51.2679772) -> Point(6.8250859, 51.2002376)
        }
    }
    }

  }
