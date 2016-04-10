package io.plasmap.query.engine.test

import io.plasmap.model.geometry.{MultiPolygon, GeometryCollection}
import io.plasmap.model.{OsmId, OsmDenormalizedRelation}
import io.plasmap.util.GeoCalculator
import org.specs2.mutable.Specification

import scala.util.Random

/**
  * Created by mark on 04.12.15.
  */
object GeoCalculatorSpec extends Specification {
  def fakeGeo(shape:List[(Double, Double)]) = {
    GeometryCollection(
      List(
        MultiPolygon(
          List(
            List(
              shape
            )
          )
        )
      )
    )
  }

  def mockRelation(geos:(Double, Double)*) = {
    OsmDenormalizedRelation(id = OsmId(Random.nextInt), tags = Nil, geometry = fakeGeo(geos.toList))
  }

  val city = mockRelation(
    ( 0.0, 0.0),
    ( 1.0, 0.0),
    ( 1.0, 1.0),
    ( 0.0, 1.0),
    ( 0.0, 0.0)
  )

  def district(moveBy:Double) = mockRelation(
    ( 0.0+moveBy, 0.0),
    ( 1.0+moveBy, 0.0),
    ( 1.0+moveBy, 1.0),
    ( 0.0+moveBy, 1.0),
    ( 0.0+moveBy, 0.0)
  )

  def test(percentOutside:Double) = GeoCalculator.within(district(percentOutside), city)
  "Our Geo calculator" should {

    "consider inner polygons as being inner" in {
      test(0.0) must beTrue
    }

    "consider polygons that only overlap 90 percent as being inner" in {
      test(0.1) must beTrue
    }

    "consider polygons that overlap more than 90 percent as NOT being inner" in {
      test(0.2) must beFalse
    }

  }
}
