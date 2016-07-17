package io.plasmap.util

import com.vividsolutions.jts.geom.{Envelope, Coordinate => JTSCoordinate, CoordinateSequence => JTSCoordinateSequence, Geometry => JTSGeometry, LinearRing => JTSLinearRing, Point => JTSPoint, Polygon => JTSPolygon}
import io.plasmap.geohash._
import io.plasmap.model.{OsmDenormalizedRelation, OsmId}
import io.plasmap.model.geometry._
import io.plasmap.util.GeowGeometryToJTSGeometry._
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.referencing.GeodeticCalculator

import scala.util.{Failure, Success, Try}


/**
  * Utility class to perform geometric calculations on the geow osm model.
  *
  * @author Jan Schulte <jan@plasmap.io>
  */
object GeoCalculator {

  private val geometryFactory = JTSFactoryFinder.getGeometryFactory(null)

  def calculatorForPrecision(precision: Precision): GeoHash = precision match {
    case PrecisionYottaLow_8BIT => GeoHash.yottaLow_8BIT
    case PrecisionYottaLow_6BIT => GeoHash.yottaLow_6BIT
    case PrecisionYottaLow_4BIT => GeoHash.yottaLow_4BIT
    case PrecisionYottaLow_2BIT => GeoHash.yottaLow_2BIT
    case PrecisionUltraLow_630KM => GeoHash.ultraLow
    case PrecisionUltraLow_12BIT => GeoHash.ultraLow_12BIT
    case PrecisionUltraLow_14BIT => GeoHash.ultraLow_14BIT
    case PrecisionVeryLow_80KM => GeoHash.veryLow
    case PrecisionVeryLow_18BIT => GeoHash.veryLow_18BIT
    case PrecisionLow_20KM => GeoHash.low
    case PrecisionLow_22BIT => GeoHash.low_22BIT
    case PrecisionLow_24BIT => GeoHash.low_24BIT
    case PrecisionMedium_5KM => GeoHash.medium
    case PrecisionMedium_28BIT => GeoHash.medium_28BIT
    case PrecisionMedium_30BIT => GeoHash.medium_30BIT
    case PrecisionMedium_32BIT => GeoHash.medium_32BIT
    case PrecisionMedium_34BIT => GeoHash.medium_34BIT
    case PrecisionHigh_100M => GeoHash.high
    case PrecisionHigh_38BIT => GeoHash.high_38BIT
    case PrecisionHigh_40BIT => GeoHash.high_40BIT
    case PrecisionHigh_42BIT => GeoHash.high_42BIT
    case PrecisionHigh_44BIT => GeoHash.high_44BIT
    case PrecisionHigh_46BIT => GeoHash.high_46BIT
    case PrecisionVeryHigh_1M => GeoHash.veryHigh
    case PrecisionVeryHigh_50BIT => GeoHash.veryHigh_50BIT
    case PrecisionVeryHigh_52BIT => GeoHash.veryHigh_52BIT
    case PrecisionVeryHigh_54BIT => GeoHash.veryHigh_54BIT
    case PrecisionVeryHigh_56BIT => GeoHash.veryHigh_56BIT
    case PrecisionVeryHigh_58BIT => GeoHash.veryHigh_58BIT
    case PrecisionUltra_1CM => GeoHash.ultra
    case PrecisionUltra_62BIT => GeoHash.ultra_62
    case PrecisionUltraHigh_1MM => GeoHash.ultraHigh
  }

  def radiusToBoundingBoxes: () => (Point, Double, Precision) => List[Long] = () => {
    val geodeticCalculator = new GeodeticCalculator()
    val pointHasher = GeoHash.ultra

    (point, radius, targetPrecision) => {

      geodeticCalculator.setStartingGeographicPoint(point.lon, point.lat)

      geodeticCalculator.setDirection(-90, radius)
      val left = geodeticCalculator.getDestinationGeographicPoint

      geodeticCalculator.setDirection(90, radius)
      val right = geodeticCalculator.getDestinationGeographicPoint

      geodeticCalculator.setDirection(0, radius)
      val top = geodeticCalculator.getDestinationGeographicPoint

      geodeticCalculator.setDirection(180, radius)
      val bottom = geodeticCalculator.getDestinationGeographicPoint

      val upperLeftUltra = pointHasher.encodeParallel(left.getX, top.getY)
      val upperLeft = pointHasher.reduceParallelPrecision(upperLeftUltra, targetPrecision)

      val lowerRightUltra = pointHasher.encodeParallel(right.getX, bottom.getY)
      val lowerRight = pointHasher.reduceParallelPrecision(lowerRightUltra, targetPrecision)

      calculatorForPrecision(targetPrecision)
        .encapsulatingRectangleHashes(upperLeft, lowerRight)
        .toList
        .flatMap(_.toList)
    }
  }

  def orthodromicDistance: () => (Point, Point) => Double = () => {
    val geodeticCalculator = new GeodeticCalculator()

    (start, destination) => {
      geodeticCalculator.setStartingGeographicPoint(start.lon, start.lat)
      geodeticCalculator.setDestinationGeographicPoint(destination.lon, destination.lat)
      geodeticCalculator.getOrthodromicDistance
    }

  }

  def multiPolysFromGeoColl(gc: GeometryCollection): List[MultiPolygon] = {
    gc.geometries.collect { case mp: MultiPolygon ⇒ mp } //TODO: Might have to flatten first.
  }

  def within(inner: Geometry, outer: Geometry): Boolean = (inner, outer) match {
    case (innerGeometry: GeometryCollection, outerGeometry: GeometryCollection) =>
      val innerMps: List[MultiPolygon] = multiPolysFromGeoColl(innerGeometry)
      val outerMps: List[MultiPolygon] = multiPolysFromGeoColl(outerGeometry)
      val isWithin = innerMps.forall(p => outerMps.exists(q => p.within(q)))
      if (isWithin) true
      else {
        val isAtLeast90PercentIn = fuzzyWithin(outerMps, innerMps)
        isAtLeast90PercentIn
      }

    case (point: Point, gc: GeometryCollection) =>

      val mps = multiPolysFromGeoColl(gc)
      val jtsPoint = geometryFactory.createPoint(new JTSCoordinate(point.lon, point.lat))
      mps.exists(_.contains(jtsPoint))
    case x =>
      //TODO implement
      println(s"Received $x")
      false
  }

  def fuzzyWithin(outerMps: List[MultiPolygon], innerMps: List[MultiPolygon]): Boolean = {
    innerMps.forall(p => outerMps.exists(q =>
      Try {
        //println(s"Checking if $p is within $q")
        val intersection: JTSGeometry = q.intersection(p)
        (intersection.getArea / p.getArea) >= 0.90
      } match {
        case Success(isWithin) => isWithin
        case Failure(ex) =>
          ex.printStackTrace()
          false
      }
    ))
  }

  def rectangle(geometry: GeometryCollection): List[(Point, Point)] = {

    //val coordsOpt: Option[List[List[(Double, Double)]]] = multiPolysFromGeoColl(geometry).headOption.flatMap(_.coordinates.headOption)
    val coords: List[List[List[(Double, Double)]]] = multiPolysFromGeoColl(geometry).flatMap(_.coordinates)
    val polygons: List[JTSPolygon] = coords.map((x: List[List[(Double, Double)]]) ⇒ doubleTupleListList2JTSPolygon(x))
    polygons match {
      case Nil =>
        List(rectangleFromOthers(geometry))
      case polygons: List[JTSPolygon] =>
        polygons.map(rectangleFromPolygon)
    }

  }

  def rectangleFromPolygon(polygon: JTSPolygon): (Point, Point) = {

    val envelope: Envelope = polygon.getEnvelopeInternal

    val upperLeft = Point(envelope.getMinX, envelope.getMaxY)
    val lowerRight = Point(envelope.getMaxX, envelope.getMinY)
    (upperLeft, lowerRight)

  }

  def rectangleFromOthers(geometry: GeometryCollection): (LonLatPoint, LonLatPoint) = {
    val points: List[Point] = (for {
      geometry <- geometry.geometries
    } yield geometry match {
      case MultiPolygon(coordinates) =>
        coordinates.flatten.flatten.map(point => Point(point._1, point._2))
      case p: Point =>
        List(p)
      case LineString(coordinates) =>
        coordinates.map(point => Point(point._1, point._2))
      case _ =>
        List.empty[Point]
    }).flatten

    val lonSorted = points.sortBy(_.lon)
    val latSorted = points.sortBy(_.lat)

    (lonSorted, latSorted) match {
      case (minLon :: lonTail, minLat :: latTail) => (lonSorted.reverse, latSorted.reverse) match {
        case (maxLon :: lonTail2, maxLat :: latTail2) =>
          Point(minLon.lon, maxLat.lat) -> Point(maxLon.lon, minLat.lat)

        case _ => Point(6.7406842, 51.2679772) -> Point(6.8250859, 51.2002376)
      }

      case _ => Point(6.7406842, 51.2679772) -> Point(6.8250859, 51.2002376)
    }
  }
}

