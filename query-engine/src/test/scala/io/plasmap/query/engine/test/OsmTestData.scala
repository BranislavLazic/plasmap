package io.plasmap.query.engine.test

import io.plasmap.model.{OsmDenormalizedObject, OsmDenormalizedRelation}
import io.plasmap.parser.impl.OsmGeoJSONBoundaryParser

import scala.io.Codec
import scala.util.Random

/**
 * Created by janschulte on 17/07/15.
 */
trait OsmTestData {

  private val relations: PartialFunction[OsmDenormalizedObject, OsmDenormalizedRelation] = {
    case rel: OsmDenormalizedRelation => rel
  }

  val pathPrefix = "query-engine/src/test/resources/"

  implicit val codec = Codec.UTF8

  val cityEssenParser = OsmGeoJSONBoundaryParser(pathPrefix + "city.essen.geojson")
  val cityMuehlheimParser = OsmGeoJSONBoundaryParser(pathPrefix + "city.muehlheim.geojson")
  val cityDuisburgParser = OsmGeoJSONBoundaryParser(pathPrefix + "city.duisburg.geojson")
  val districtsEssenParser = OsmGeoJSONBoundaryParser(pathPrefix + "districts.essen.geojson")
  val districtsMuehlheimParser = OsmGeoJSONBoundaryParser(pathPrefix + "districts.muehlheim.geojson")
  val districtsDuisburgParser = OsmGeoJSONBoundaryParser(pathPrefix + "districts.duisburg.geojson")

  val restaurantsFile = scala.io.Source.fromFile(pathPrefix + "restaurants.geojson")
  val theatresFile    = scala.io.Source.fromFile(pathPrefix + "theatres.geojson")

  val cityEssen: List[OsmDenormalizedRelation] = (for {opt <- cityEssenParser
  } yield opt).flatten.collect(relations).toList
  val cityMuehlheim: List[OsmDenormalizedRelation] = (for {opt <- cityMuehlheimParser
  } yield opt).flatten.collect(relations).toList
  val cityDuisburg: List[OsmDenormalizedRelation] = (for {opt <- cityDuisburgParser
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
