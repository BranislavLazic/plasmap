package io.plasmap.util.test

import io.plasmap.model.{OsmDenormalizedObject, OsmDenormalizedRelation}
import io.plasmap.parser.impl.OsmGeoJSONBoundaryParser

import scala.io.Codec
import scala.util.Random

/**
 * Utility trait containing real, parsed test data
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
trait OsmTestData {

  private[this] val relations: PartialFunction[OsmDenormalizedObject, OsmDenormalizedRelation] = {
    case rel: OsmDenormalizedRelation => rel
  }

  private[this] val cityEssenParser = OsmGeoJSONBoundaryParser("util/src/test/resources/city.essen.geojson")(Codec.UTF8)
  private[this] val cityMuehlheimParser = OsmGeoJSONBoundaryParser("util/src/test/resources/city.muehlheim.geojson")(Codec.UTF8)
  private[this] val cityDuisburgParser = OsmGeoJSONBoundaryParser("util/src/test/resources/city.duisburg.geojson")(Codec.UTF8)
  private[this] val districtsEssenParser = OsmGeoJSONBoundaryParser("util/src/test/resources/districts.essen.geojson")(Codec.UTF8)
  private[this] val districtsMuehlheimParser = OsmGeoJSONBoundaryParser("util/src/test/resources/districts.muehlheim.geojson")(Codec.UTF8)
  private[this] val districtsDuisburgParser = OsmGeoJSONBoundaryParser("util/src/test/resources/districts.duisburg.geojson")(Codec.UTF8)

  private[this] val cityEssen: List[OsmDenormalizedRelation] = (for {opt <- cityEssenParser
  } yield opt).flatten.collect(relations).toList
  private[this] val cityMuehlheim: List[OsmDenormalizedRelation] = (for {opt <- cityMuehlheimParser
  } yield opt).flatten.collect(relations).toList
  private[this] val cityDuisburg: List[OsmDenormalizedRelation] = (for {opt <- cityDuisburgParser
  } yield opt).flatten.collect(relations).toList

  val districtsEssen: List[OsmDenormalizedRelation] = (for {opt <- districtsEssenParser
  } yield opt).flatten.collect(relations).toList
  val districtsMuehlheim: List[OsmDenormalizedRelation] = (for {opt <- districtsMuehlheimParser
  } yield opt).flatten.collect(relations).toList
  val districtsDuisburg: List[OsmDenormalizedRelation] = (for {opt <- districtsDuisburgParser
  } yield opt).flatten.collect(relations).toList

  val essen = cityEssen(0)
  val muehlheim = cityMuehlheim(0)
  val duisburg = cityDuisburg(0)

  val essenMuehlheimDistricts: List[OsmDenormalizedRelation] = Random.shuffle(districtsEssen.union(districtsMuehlheim))
  val duisburgMuehlheimDistricts: List[OsmDenormalizedRelation] = Random.shuffle(districtsDuisburg.union(districtsMuehlheim))
}
