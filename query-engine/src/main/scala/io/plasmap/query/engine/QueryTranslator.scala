package io.plasmap.query.engine

import _root_.io.plasmap.querymodel._
import akka.stream.scaladsl.FlowGraph
import akka.stream._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scalaz.syntax.either._
import scalaz.{-\/, \/, \/-}


/**
 * Utility class to translate a frontend query to a backend query.
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
object QueryTranslator {

  val log = Logger(LoggerFactory.getLogger(QueryTranslator.getClass.getName))

  sealed trait TranslationError

  final case class NotYetImplemented(q: PMQuery) extends TranslationError

  final case object CouldNotCreateInputQuery extends TranslationError

  final case object UnknownCommand extends TranslationError

  def translate(pmQuery: PMQuery)(implicit mat:Materializer, ec:ExecutionContext): TranslationError \/ Query[_ <: Shape, _] = {
    log.info("translating query: $pmQuery")

    def poiAreaMatch[A](area:PMAreaQuery)(implicit instance:POI[A]) = {
      translate(area) match {
        case \/-(aq: AreaQuery[_]) =>
          PointOfInterestQuery.fromArea(aq).right
        case \/-(x) => CouldNotCreateInputQuery.left
        case -\/(_) => CouldNotCreateInputQuery.left
      }
    }

    pmQuery match {

      case PMCoordinates(lon, lat) =>
        CoordinatesQuery(lon, lat).right

      case PMCountryFromCoordinates(PMCoordinates(lon, lat)) =>
        CountryQuery(lon, lat).right

      case PMCountryFromName(name) =>
        CountryQuery(name).right

      case PMStateFromCoordinates(PMCoordinates(lon, lat)) =>
        StateQuery(lon, lat).right

      case PMStateFromName(name) =>
        StateQuery(name).right

      case PMStateFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => StateQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMRegionFromCoordinates(PMCoordinates(lon, lat)) =>
        RegionQuery(lon, lat).right

      case PMRegionFromName(name) =>
        RegionQuery(name).right

      case PMRegionFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => RegionQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMCityFromCoordinates(PMCoordinates(lon, lat)) =>
        CityQuery(lon, lat).right

      case PMCityFromName(name) =>
        CityQuery(name).right

      case PMCityFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => CityQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMTownshipFromCoordinates(PMCoordinates(lon, lat)) =>
        TownshipQuery(lon, lat).right

      case PMTownshipFromName(name) =>
        TownshipQuery(name).right

      case PMTownshipFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => TownshipQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMDistrictFromCoordinates(PMCoordinates(lon, lat)) =>
        DistrictQuery(lon, lat).right

      case PMDistrictFromName(name) =>
        DistrictQuery(name).right

      case PMDistrictsFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => DistrictQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMVillageFromCoordinates(PMCoordinates(lon, lat)) =>
        VillageQuery(lon, lat).right

      case PMVillageFromName(name) =>
        VillageQuery(name).right

      case PMVillageFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => VillageQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMCommunityFromCoordinates(PMCoordinates(lon, lat)) =>
        CommunityQuery(lon, lat).right

      case PMCommunityFromName(name) =>
        CommunityQuery(name).right

      case PMCommunityFromArea(area) =>
        translate(area) match {
          case \/-(aq: AreaQuery[_]) => CommunityQuery(aq).right
          case \/-(x)             => CouldNotCreateInputQuery.left
          case -\/(_)             => CouldNotCreateInputQuery.left
        }

      case PMBarsFromArea               (area) => poiAreaMatch(area)(POIs.poiBars)
      case PMBarbecuesFromArea          (area) => poiAreaMatch(area)(POIs.poiBarbecues)
      case PMBiergartensFromArea        (area) => poiAreaMatch(area)(POIs.poiBiergartens)
      case PMCafesFromArea              (area) => poiAreaMatch(area)(POIs.poiCafes)
      case PMFastFoodFromArea           (area) => poiAreaMatch(area)(POIs.poiFastFood)
      case PMIceCreamParloursFromArea   (area) => poiAreaMatch(area)(POIs.poiIceCreamParlours)
      case PMPubsFromArea               (area) => poiAreaMatch(area)(POIs.poiPubs)
      case PMRestaurantsFromArea(area)         => poiAreaMatch(area)(POIs.poiRestaurant)
      case PMSupermarketsFromArea(area)        => poiAreaMatch(area)(POIs.poiSupermarket)
      case PMMuseumsFromArea(area)             => poiAreaMatch(area)(POIs.poiMuseum)
      case PMTheatresFromArea(area)            => poiAreaMatch(area)(POIs.poiTheatre)
      case PMKindergartensFromArea(area)       => poiAreaMatch(area)(POIs.poiKindergarten)
      case PMPublicTransportStopsFromArea(area)=> poiAreaMatch(area)(POIs.poiPublicTransportStopPosition)
      case PMCollegesFromArea(area)            => poiAreaMatch(area)(POIs.poiCollege)
      case PMLibrariesFromArea(area)           => poiAreaMatch(area)(POIs.poiLibrary)
      case PMSchoolsFromArea(area)             => poiAreaMatch(area)(POIs.poiSchool)
      case PMUniversitiesFromArea(area)        => poiAreaMatch(area)(POIs.poiUniversity)
      case PMPetrolStationsFromArea(area)      => poiAreaMatch(area)(POIs.poiPetrolStation)
      case PMParkingsFromArea(area)            => poiAreaMatch(area)(POIs.poiParking)
      case PMTaxisFromArea(area)               => poiAreaMatch(area)(POIs.poiTaxi)
      case PMATMsFromArea(area)                => poiAreaMatch(area)(POIs.poiATM)
      case PMBanksFromArea(area)               => poiAreaMatch(area)(POIs.poiBank)
      case PMBureauxDeChangeFromArea(area)     => poiAreaMatch(area)(POIs.poiBureauDeChange)
      case PMClinicsFromArea(area)             => poiAreaMatch(area)(POIs.poiClinic)
      case PMDentistsFromArea(area)            => poiAreaMatch(area)(POIs.poiDentist)
      case PMDoctorsFromArea(area)             => poiAreaMatch(area)(POIs.poiDoctor)
      case PMHospitalsFromArea(area)           => poiAreaMatch(area)(POIs.poiHospital)
      case PMPharmaciesFromArea(area)          => poiAreaMatch(area)(POIs.poiPharmacy)
      case PMVeterinariesFromArea(area)        => poiAreaMatch(area)(POIs.poiVeterinary)
      case PMBrothelsFromArea(area)            => poiAreaMatch(area)(POIs.poiBrothel)
      case PMCasinosFromArea(area)             => poiAreaMatch(area)(POIs.poiCasino)
      case PMCinemasFromArea(area)             => poiAreaMatch(area)(POIs.poiCinema)
      case PMNightClubsFromArea(area)          => poiAreaMatch(area)(POIs.poiNightClub)
      case PMStripClubsFromArea(area)          => poiAreaMatch(area)(POIs.poiStripClub)
      case PMStudiosFromArea(area)             => poiAreaMatch(area)(POIs.poiStudio)
      case PMCoworkingSpacesFromArea(area)     => poiAreaMatch(area)(POIs.poiCoworkingSpace)
      case PMFireStationsFromArea(area)        => poiAreaMatch(area)(POIs.poiFireStation)
      case PMGymsFromArea(area)                => poiAreaMatch(area)(POIs.poiGym)
      case PMPlacesOfWorshipFromArea(area)     => poiAreaMatch(area)(POIs.poiPlaceOfWorship)
      case PMPoliceStationsFromArea(area)      => poiAreaMatch(area)(POIs.poiPoliceStation)
      case PMPostBoxesFromArea(area)           => poiAreaMatch(area)(POIs.poiPostBox)
      case PMPostOfficesFromArea(area)         => poiAreaMatch(area)(POIs.poiPostOffice)
      case PMPrisonsFromArea(area)             => poiAreaMatch(area)(POIs.poiPrison)
      case PMRecyclingContainersFromArea(area) => poiAreaMatch(area)(POIs.poiRecyclingContainer)
      case PMSaunasFromArea(area)              => poiAreaMatch(area)(POIs.poiSauna)
      case PMTelephonesFromArea(area)          => poiAreaMatch(area)(POIs.poiTelephone)
      case PMToiletsFromArea(area)             => poiAreaMatch(area)(POIs.poiToilet)
      case PMGolfCoursesFromArea(area)         => poiAreaMatch(area)(POIs.poiGolfCourse)
      case PMIceRinksFromArea(area)            => poiAreaMatch(area)(POIs.poiIceRink)
      case PMParksFromArea(area)               => poiAreaMatch(area)(POIs.poiPark)
      case PMSportPitchesFromArea(area)        => poiAreaMatch(area)(POIs.poiSportPitch)
      case PMPlaygroundsFromArea(area)         => poiAreaMatch(area)(POIs.poiPlayground)
      case PMStadiumsFromArea(area)            => poiAreaMatch(area)(POIs.poiStadium)
      case PMSwimmingPoolsFromArea(area)       => poiAreaMatch(area)(POIs.poiSwimmingPool)
      case PMSwimmingAreasFromArea(area)       => poiAreaMatch(area)(POIs.poiSwimmingArea)
      case PMSportTracksFromArea(area)         => poiAreaMatch(area)(POIs.poiSportTrack)
      case PMTreesFromArea(area)               => poiAreaMatch(area)(POIs.poiTree)


      case unknown =>
        log.trace(s"Could not parse command $unknown")
        UnknownCommand.left
    }
  }


}
