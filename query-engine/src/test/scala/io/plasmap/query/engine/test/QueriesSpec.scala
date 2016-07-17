package io.plasmap.query.engine.test

import io.plasmap.model._
import _root_.io.plasmap.query.engine._
import _root_.io.plasmap.serializer.OsmDenormalizedSerializer
import akka.NotUsed
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import io.plasmap.geo.mappings.{IndexSearchHit, IndexingService}
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.{IsolatedMockFactory, MockContext}
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Codec
import scalaz.syntax.id._

/**
  * Specification for Queries
  */
class QueriesSpec extends Specification
  with IsolatedMockFactory
  with ProxyMockFactory {

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global
  import Queries._
  import io.plasmap.util.test.OsmTestData._


  val pathPrefix = "query-engine/src/test/resources/"

  implicit val codec = Codec.UTF8

  val restaurantsFile = scala.io.Source.fromFile(pathPrefix + "restaurants.geojson")
  val theatresFile    = scala.io.Source.fromFile(pathPrefix + "theatres.geojson")

  "The Queries" should {

    "retrieve a relation by name and tag" in new MockContext {


      private val osmId: OsmId = OsmId(123456789L)
      val id = Id(osmId)
      val bb = BoundingBox(1001231231312L)
      val name = "Essen"

      val indexingService = stub[IndexingService]
      indexingService.searchOsmObjectSource _ when(name, OsmTypeRelation) returns Source.single(IndexSearchHit(osmId, 1.0f))

      val toIndex = stubFunction[Name, Future[List[Id]]]
      val toBoundingBox = stubFunction[Id, Future[List[BoundingBox]]]
      val toData = stubFunction[BoundingBox, Id, Future[List[OsmDenormalizedObject]]]

      toBoundingBox when id returns Future.successful {
        List(bb)
      } once()

      toData when(bb, id) returns Future.successful {
        List(essen)
      } once()

      val actualRelation = relationByNameAndType(name, cityTag, indexingService,
        toBoundingBox,
        toData
      ).runReduce((acc, elem) => elem)

      Await.result(actualRelation, 5 seconds) must be_==(essen)
    }

    "retrieve a relation by location" in {

      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]

      val (lon, lat) = 6.757042407989502 -> 51.43089564458017
      val bb = BoundingBox(-3431461441079607296L)
      val tag = Tag(OsmTag("admin_level", "6"))

      toData when(bb, tag) returns Future.successful {
        List(duisburg)
      } once()

      val source: Source[OsmDenormalizedRelation, NotUsed] = relationByCoordinatesAndType(
        lon,
        lat,
        cityTag,
        toData
      )

      val actualRelation: Future[OsmDenormalizedRelation] = source.runReduce((acc, elem) => elem)

      Await.result(actualRelation, 5 seconds) must be_==(duisburg)
    }

    val bbDuisburg = BoundingBox(-3431461441079607296L)
    val bbEssen = BoundingBox(-3430898491126185984L)
    val tag = Tag(OsmTag("admin_level", "8"))

    val testData = List(
      duisburg -> districtsDuisburg,
      muehlheim -> districtsMuehlheim,
      essen -> districtsEssen
    )

    "retrieve a relation by containment" in forall(testData){ tuple =>

      val (city, expectedDistricts) = tuple

      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]

      toData when(bbDuisburg, tag) returns Future.successful { duisburgMuehlheimDistricts }
      toData when(bbEssen, tag) returns Future.successful { essenMuehlheimDistricts }
      toData when(*,*) returns Future.successful { List.empty } never()

      val areaQuery = CityQuery(Source.single(City(city)))

      val districtSource = relationByContainment(areaQuery, districtTag, District, toData)

      val eventualDistricts = districtSource.runFold(List.empty[District])((list, district) => {
        district :: list
      })
      val actualDistricts = Await.result(eventualDistricts, 5 seconds)
      actualDistricts must containAllOf(expectedDistricts.map(District))
    }
  }

  "The POIQueries" should {

    implicit val code = Codec.UTF8
    val restaurants: List[OsmDenormalizedObject] = restaurantsFile.mkString ▹ OsmDenormalizedSerializer.fromGeoJsonString
    val theatres: List[OsmDenormalizedObject] = theatresFile.mkString ▹ OsmDenormalizedSerializer.fromGeoJsonString

    "retrieve restaurants with a POIQuery" in {
      //Get typeclass instance for POI[Restaurant]
      import POIs.poiRestaurant
      val toData = stubFunction[BoundingBox, Tag, Future[List[OsmDenormalizedObject]]]
      toData when(*, *) returns Future {
        restaurants
      }

      val restaurantSource = Source.fromGraph(
        PointOfInterestQuery.fromArea(CityQuery(Source.single(City(duisburg))), toData).shape
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
      toData when(*, *) returns Future {
        theatres
      }

      val theatreSource = Source.fromGraph(
        PointOfInterestQuery.fromArea(CityQuery(Source.single(City(duisburg))), toData).shape
      )

      val eventualTheatres: Future[Set[Theatre]] = theatreSource.runFold(List.empty[Theatre])((list, theatre) ⇒ {
        theatre :: list
      }).map(_.toSet)

      Await.result(eventualTheatres, 60 seconds) must have size 2
    }
  }
}
