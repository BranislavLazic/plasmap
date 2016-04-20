package io.plasmap.query.engine

import akka.stream.{SourceShape, Graph}
import io.plasmap.geohash.GeoHash
import io.plasmap.model.{OsmTag, OsmDenormalizedObject, OsmDenormalizedRelation}
import io.plasmap.query.engine.TypeAliases.SourceGraph
import io.plasmap.util.GeoCalculator
import io.plasmap.query.engine.Queries._

import scala.language.experimental.macros
import io.plasmap.queryengine.macros.Macros._

/**
  * This contains type class instances for the POI type class.
  * Import all via import POIs._ or specify the ones you need
  * as in import POIs.poiTheatre
  *
  * Created by mark on 13.10.15.
  */
object POIs {

  //Helper method (Don't move down.)
  def bbsToQueryFromTag(rel: OsmDenormalizedRelation, key: String, value: String): List[(BoundingBox, Tag)] = {
    val tag = OsmTag(key, value)
    Queries.createBBTag(rel, tag)
  }

  def bbsToQueryFromTags(rel: OsmDenormalizedRelation, kvs: Map[String, String]): List[(BoundingBox, Tag)] = {
    for {
      (k, v) <- kvs.toList
      li <- bbsToQueryFromTag(rel, k, v)
    } yield li
  }

  //Keep this for the macro macro macro
  import io.plasmap.query.engine.POIQueries._

  //This is the function that invokes the macro.
  def tci[A](tagKey: String, tagVal: String): POI[A] =
  macro poiTypeClassInstanceMacro[A]

  implicit val poiBars =
    tci[Bar]("amenity", "bar")

  implicit val poiBarbecues =
    tci[Barbecue]("amenity", "bbq")

  implicit val poiBiergartens =
    tci[Biergarten]("amenity", "biergarten")

  implicit val poiCafes =
    tci[Cafe]("amenity", "cafe")

  implicit val poiFastFood =
    tci[FastFood]("amenity", "fast_food")

  implicit val poiIceCreamParlours =
    tci[IceCreamParlour]("amenity", "ice_cream")

  implicit val poiPubs =
    tci[Pub]("amenity", "pub")

  implicit val poiTheatre =
    tci[Theatre]("amenity", "theatre")

  implicit val poiSupermarket =
    tci[Supermarket]("shop", "supermarket")

  implicit val poiMuseum =
    tci[Museum]("tourism", "museum")

  implicit val poiKindergarten =
    tci[Kindergarten]("amenity", "kindergarten")

  implicit val poiPublicTransportStopPosition =
    tci[PublicTransportStopPosition]("public_transport", "stop_position")

  implicit val poiCollege =
    tci[College]("amenity", "college")

  implicit val poiLibrary =
    tci[Library]("amenity", "library")

  implicit val poiSchool =
    tci[School]("amenity", "school")

  implicit val poiUniversity =
    tci[University]("amenity", "university")

  implicit val poiPetrolStation =
    tci[PetrolStation]("amenity", "fuel")

  implicit val poiParking =
    tci[Parking]("amenity", "parking")

  implicit val poiTaxi =
    tci[Taxi]("amenity", "taxi")

  implicit val poiATM =
    tci[ATM]("amenity", "atm")

  implicit val poiBank =
    tci[Bank]("amenity", "bank")

  implicit val poiBureauDeChange =
    tci[BureauDeChange]("amenity", "bureau_de_change")

  implicit val poiClinic =
    tci[Clinic]("amenity", "clinic")

  implicit val poiDentist =
    tci[Dentist]("amenity", "dentist")

  implicit val poiDoctor =
    tci[Doctor]("amenity", "doctors")

  implicit val poiHospital =
    tci[Hospital]("amenity", "hospital")

  implicit val poiPharmacy =
    tci[Pharmacy]("amenity", "pharmacy")

  implicit val poiVeterinary =
    tci[Veterinary]("amenity", "veterinary")

  implicit val poiBrothel =
    tci[Brothel]("amenity", "brothel")

  implicit val poiCasino =
    tci[Casino]("amenity", "casino")

  implicit val poiCinema =
    tci[Cinema]("amenity", "cinema")

  implicit val poiNightClub =
    tci[NightClub]("amenity", "nightclub")

  implicit val poiStripClub =
    tci[StripClub]("amenity", "stripclub")

  implicit val poiStudio =
    tci[Studio]("amenity", "studio")

  implicit val poiCoworkingSpace =
    tci[CoworkingSpace]("amenity", "coworking_space")

  implicit val poiFireStation =
    tci[FireStation]("amenity", "fire_station")

  implicit val poiGym: POI[Gym] = new POI[Gym] {
    private val internalTags = List("amenity" -> "gym",
    "leisure" -> "gym",
    "leisure" -> "sports_centre",
    "sport" -> "fitness")

    def tags = internalTags.map((x) => OsmTag(x._1,x._2))

    def osmObj(g: Gym): OsmDenormalizedObject = g.osmObject

    def bbsToQuery(rel: OsmDenormalizedRelation): List[(BoundingBox, Tag)] =
      bbsToQueryFromTags(rel,
        internalTags.toMap
      )

    def queryFromShape(shape: SourceGraph[Gym]): POIQuery[Gym] =
      POIQueryGym(shape)

    def fromOsmDenObj(osmObj: OsmDenormalizedObject): Gym =
      Gym(osmObj)

    val name = "gyms"
  }

  implicit val poiPlaceOfWorship =
    tci[PlaceOfWorship]("amenity", "place_of_worship")

  implicit val poiPoliceStation =
    tci[PoliceStation]("amenity", "police")

  implicit val poiPostBox =
    tci[PostBox]("amenity", "post_box")

  implicit val poiPostOffice =
    tci[PostOffice]("amenity", "post_office")

  implicit val poiPrison =
    tci[Prison]("amenity", "prison")

  implicit val poiRecyclingContainer =
    tci[RecyclingContainer]("amenity", "recycling")

  implicit val poiSauna =
    tci[Sauna]("amenity", "sauna")

  implicit val poiTelephone =
    tci[Telephone]("amenity", "telephone")

  implicit val poiToilet =
    tci[Toilet]("amenity", "toilets")

  implicit val poiGolfCourse =
    tci[GolfCourse]("leisure", "golf_course")

  implicit val poiIceRink =
    tci[IceRink]("leisure", "ice_rink")

  implicit val poiPark =
    tci[Park]("leisure", "park")

  implicit val poiSportPitch =
    tci[SportPitch]("leisure", "pitch")

  implicit val poiPlayground =
    tci[Playground]("leisure", "playground")

  implicit val poiStadium =
    tci[Stadium]("leisure", "stadium")

  implicit val poiSwimmingPool =
    tci[SwimmingPool]("leisure", "swimming_pool")

  implicit val poiSwimmingArea =
    tci[SwimmingArea]("leisure", "swimming_area")

  implicit val poiSportTrack =
    tci[SportTrack]("leisure", "track")

  implicit val poiTree =
    tci[Tree]("natural", "tree")


  //Reference implementation of the POI instance for Restaurant
  //Left here instead of being done with the macro for reference
  //when building more sophisticated/complicated type classes.

  implicit val poiRestaurant: POI[Restaurant] = new POI[Restaurant] {
    def tags = List(OsmTag("amenity", "restaurant"))

    def osmObj(r: Restaurant): OsmDenormalizedObject =
      r.osmObject

    def bbsToQuery(rel: OsmDenormalizedRelation): List[(BoundingBox, Tag)] =
      bbsToQueryFromTag(rel, "amenity", "restaurant")

    def queryFromShape(shape: SourceGraph[Restaurant]): POIQuery[Restaurant] =
      POIQueryRestaurant(shape)

    def fromOsmDenObj(osmObj: OsmDenormalizedObject): Restaurant =
      Restaurant(osmObj)

    val name = "restaurants"
  }

}
