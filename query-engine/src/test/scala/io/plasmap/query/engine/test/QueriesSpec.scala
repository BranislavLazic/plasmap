package io.plasmap.query.engine.test

import _root_.io.plasmap.model.{OsmDenormalizedObject, OsmId, OsmTag}
import _root_.io.plasmap.query.engine._
import _root_.io.plasmap.serializer.OsmDenormalizedSerializer
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Codec
import scalaz.syntax.id._
/**
 * Specification for Queries
 */
class QueriesSpec extends Specification
with OsmTestData
with IsolatedMockFactory
with ProxyMockFactory {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  "The CityQuery" should {
    "retrieve a city by name" in {

      val toIndex = stubFunction[Name, Future[List[Id]]]
      val toBoundingBox = stubFunction[Id, Future[List[BoundingBox]]]
      val toData = stubFunction[BoundingBox, Id, Future[List[OsmDenormalizedObject]]]

      val id = Id(OsmId(0L))
      val bb = BoundingBox(1001231231312L)
      val name: String = "Essen"

      toIndex when Name(name) returns Future { List(id) } once()

      toBoundingBox when id returns Future { List(bb) } once()

      toData when(bb, id) returns Future { List(essen) } once()

      val wrap: Source[City, Unit] = Source.wrap(CityQuery.fromName(name,
        toIndex,
        toBoundingBox,
        toData
      ).shape)

      val cities: Future[List[City]] = wrap.runFold(List.empty[City])((list, city) => {
        city :: list
      })

      Await.result(cities, 100 millis) must containTheSameElementsAs(List(City(essen)))
    }
    "retrieve a city by location" in {

      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]

      val (lon, lat) = 6.757042407989502 -> 51.43089564458017
      val bb = BoundingBox(-3431461441079607296L)
      val tag = Tag(OsmTag("admin_level", "6"))

      toData when(bb, tag) returns Future {
        List(duisburg)
      } once()

      val wrap: Source[City, Unit] = Source.wrap(CityQuery.fromLonLat(
        lon,
        lat,
        toData
      ).shape)

      val cities: Future[List[City]] = wrap.runFold(List.empty[City])((list, city) => {
        city :: list
      })

      Await.result(cities, 500 millis) must containTheSameElementsAs(List(City(duisburg)))
    }
  }

  "The DistrictQuery" should {
    "retrieve a district by city" in {

      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]

      val bbDuisburg = BoundingBox(-3431461441079607296L)
      val bbEssen = BoundingBox(-3430898491126185984L)
      val tag = Tag(OsmTag("admin_level", "10"))

      toData when(bbDuisburg, tag) returns Future {
        List.empty[OsmDenormalizedObject] ++ duisburgMuehlheimDistricts
      } once()

      toData when(bbEssen, tag) returns Future {
        List.empty[OsmDenormalizedObject] ++ essenMuehlheimDistricts
      } once()

      toData when(*, *) returns Future {
        List.empty[OsmDenormalizedObject] ++ duisburgMuehlheimDistricts
      } repeat 2

      val citySource = Source(List(City(duisburg), City(essen), City(muehlheim)))
      val districtSource = Source.wrap(DistrictQuery.fromCity(CityQuery(citySource), toData).shape)


      val districts: Future[List[District]] = districtSource.runFold(List.empty[District])((list, district) => {
        district :: list
      })

      val expectedDistrictsDuisburg: List[District] = districtsDuisburg.map(District)
      Await.result(districts, 60 seconds) must containAllOf(expectedDistrictsDuisburg)
    }
  }

  "The POIQueries" should {

    implicit val code = Codec.UTF8
    val restaurants: List[OsmDenormalizedObject] = restaurantsFile.mkString ▹ OsmDenormalizedSerializer.fromGeoJsonString
    val theatres:List[OsmDenormalizedObject]     = theatresFile   .mkString ▹ OsmDenormalizedSerializer.fromGeoJsonString
//
//    "retrieve restaurants by district" in {
//
//      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]
//      toData when(*, *) returns Future {
//        restaurants
//      }
//
//      val restaurantSource = Source.wrap(RestaurantQuery.fromArea(DistrictQuery(Source(districtsDuisburg.map(District).take(1))), toData).shape)
//
//      val eventualRestaurants: Future[Set[Restaurant]] = restaurantSource.runFold(List.empty[Restaurant])((list, restaurant) => {
//        restaurant :: list
//      }).map(_.toSet)
//
//      //val expectedRestaurants: List[Restaurant] = restaurants.map(Restaurant)
//      //Await.result(eventualRestaurants, 60 seconds) must containAllOf(expectedRestaurants)
//      Await.result(eventualRestaurants, 60 seconds) must have size 20
//    }
//
//    "retrieve restaurants by city" in {
//
//      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]
//      toData when(*, *) returns Future {
//        restaurants
//      }
//
//      val restaurantSource = Source.wrap(RestaurantQuery.fromArea(CityQuery(Source(cityDuisburg.map(City).take(1))), toData).shape)
//
//      val eventualRestaurants: Future[Set[Restaurant]] = restaurantSource.runFold(List.empty[Restaurant])((list, restaurant) => {
//        restaurant :: list
//      }).map(_.toSet)
//
//      //val expectedRestaurants: List[Restaurant] = restaurants.map(Restaurant)
//      //Await.result(eventualRestaurants, 60 seconds) must containAllOf(expectedRestaurants)
//      Await.result(eventualRestaurants, 60 seconds) must have size 125
//    }

    "retrieve restaurants with a POIQuery" in {
      //Get typeclass instance for POI[Restaurant]
      import POIs.poiRestaurant
      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]
      toData when(*, *) returns Future { restaurants }

      val restaurantSource = Source.wrap(
        PointOfInterestQuery.fromArea(CityQuery(Source(cityDuisburg.map(City).take(1))), toData).shape
      )

      val eventualRestaurants: Future[Set[Restaurant]] = restaurantSource.runFold(List.empty[Restaurant])((list, restaurant) => {
        restaurant :: list
      }).map(_.toSet)

      Await.result(eventualRestaurants, 60 seconds) must have size 125
    }

    "retrieve theatres with a POIQuery" in {
      //Get typeclass instance for POI[Theatre]
      import POIs.poiTheatre

      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]
      toData when(*, *) returns Future { theatres }

      val theatreSource = Source.wrap(
        PointOfInterestQuery.fromArea(CityQuery(Source(cityDuisburg.map(City).take(1))), toData).shape
      )

      val eventualTheatres: Future[Set[Theatre]] = theatreSource.runFold(List.empty[Theatre])((list, theatre) ⇒ {
        theatre :: list
      }).map(_.toSet)

      Await.result(eventualTheatres, 60 seconds) must have size 2
    }
  }
}
