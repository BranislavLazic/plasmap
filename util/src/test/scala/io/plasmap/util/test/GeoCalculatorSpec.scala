package io.plasmap.util.test

import io.plasmap.model.geometry.{GeometryCollection, MultiPolygon}
import io.plasmap.util.GeoCalculator
import io.plasmap.model.{OsmDenormalizedObject, OsmDenormalizedRelation}
import io.plasmap.parser.OsmDenormalizedParser
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment


class GeoCalculatorSpec extends Specification {

  import OsmTestData._


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

  def mockRelation(geos:(Double, Double)*): GeometryCollection = fakeGeo(geos.toList)

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

  "The GeoCalculator" should {

    s"calculate that Essen districts are contained in Essen" >> {
      Fragment.foreach(districtsEssen) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district.geometry,essen.geometry) must beTrue}
      }
    }

    s"calculate that Muehlheim distritcs are not contained in Essen" >> {
      Fragment.foreach(districtsMuehlheim) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district.geometry,essen.geometry) must beFalse}
      }
    }
    s"calculate that Muelheim districts are contained in Muehlheim" >> {
      Fragment.foreach(districtsMuehlheim) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district.geometry,muehlheim.geometry) must beTrue}
      }
    }

    s"calculate that Essen districts are not contained in Muehlheim" >> {
      Fragment.foreach(districtsEssen) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district.geometry,muehlheim.geometry) must beFalse}
      }
    }

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