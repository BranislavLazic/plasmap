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
object OsmTestData {

  private[this] def relations: PartialFunction[OsmDenormalizedObject, OsmDenormalizedRelation] = {
    case rel: OsmDenormalizedRelation => rel
  }

  private val testdataPath: String = "util/src/main/resources/"
  private[this] val cityEssenParser = OsmGeoJSONBoundaryParser(testdataPath + "city.essen.geojson")(Codec.UTF8)
  private[this] val cityMuehlheimParser = OsmGeoJSONBoundaryParser(testdataPath + "city.muehlheim.geojson")(Codec.UTF8)
  private[this] val cityDuisburgParser = OsmGeoJSONBoundaryParser(testdataPath + "city.duisburg.geojson")(Codec.UTF8)
  private[this] val districtsEssenParser = OsmGeoJSONBoundaryParser(testdataPath + "districts.essen.geojson")(Codec.UTF8)
  private[this] val districtsMuehlheimParser = OsmGeoJSONBoundaryParser(testdataPath + "districts.muehlheim.geojson")(Codec.UTF8)
  private[this] val districtsDuisburgParser = OsmGeoJSONBoundaryParser(testdataPath + "districts.duisburg.geojson")(Codec.UTF8)

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

  val essen = cityEssen.head
  val muehlheim = cityMuehlheim.head
  val duisburg = cityDuisburg.head

  val essenMuehlheimDistricts: List[OsmDenormalizedRelation] = Random.shuffle(districtsEssen.union(districtsMuehlheim))
  val duisburgMuehlheimDistricts: List[OsmDenormalizedRelation] = Random.shuffle(districtsDuisburg.union(districtsMuehlheim))
}
