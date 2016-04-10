package io.plasmap.util.test

import io.plasmap.util.GeoCalculator
import io.plasmap.model.{OsmDenormalizedObject, OsmDenormalizedRelation}
import io.plasmap.parser.OsmDenormalizedParser
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment


/**
 * Created by janschulte on 12/02/15.
 */
class GeoCalculatorSpec extends Specification {

  val cityEssenParser = OsmDenormalizedParser("util/src/test/resources/city.essen.geojson")
  val cityMuehlheimParser = OsmDenormalizedParser("util/src/test/resources/city.muehlheim.geojson")
  val districtsEssenParser = OsmDenormalizedParser("util/src/test/resources/districts.essen.geojson")
  val districtsMuehlheimParser = OsmDenormalizedParser("util/src/test/resources/districts.muehlheim.geojson")

  private val relations: PartialFunction[OsmDenormalizedObject, OsmDenormalizedRelation] = {
    case rel: OsmDenormalizedRelation => rel
  }

  val cityEssen:List[OsmDenormalizedRelation] = (for {opt <- cityEssenParser
  } yield opt).flatten.collect(relations).toList

  val cityMuehlheim:List[OsmDenormalizedRelation] = (for {opt <- cityMuehlheimParser
  } yield opt).flatten.collect(relations).toList

  val districtsEssen:List[OsmDenormalizedRelation] = (for {opt <- districtsEssenParser
  } yield opt).flatten.collect(relations).toList

  val districtsMuehlheim:List[OsmDenormalizedRelation] = (for {opt <- districtsMuehlheimParser
  } yield opt).flatten.collect(relations).toList

  "The GeoCalculator" should {

    val essen = cityEssen(0)
    val muehlheim = cityMuehlheim(0)

    s"calculate that Essen districts are contained in Essen" >> {
      Fragment.foreach(districtsEssen) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district,essen) must beTrue}
      }
    }

    s"calculate that Muehlheim distritcs are not contained in Essen" >> {
      Fragment.foreach(districtsMuehlheim) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district,essen) must beFalse}
      }
    }
    s"calculate that Muelheim districts are contained in Muehlheim" >> {
      Fragment.foreach(districtsMuehlheim) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district,muehlheim) must beTrue}
      }
    }

    s"calculate that Essen districts are not contained in Muehlheim" >> {
      Fragment.foreach(districtsEssen) { district =>
        s"${district.tags.filter(_.key == "name")}\n" ! { GeoCalculator.within(district,muehlheim) must beFalse}
      }
    }
  }

}