package io.plasmap.querymodel

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
sealed trait PMQuery

sealed trait PMAreaQuery                 extends PMQuery
sealed trait PMCountryQuery              extends PMAreaQuery // Level 2
sealed trait PMStateQuery                extends PMAreaQuery // Level 4
sealed trait PMRegionQuery               extends PMAreaQuery // Level 5
sealed trait PMCityQuery                 extends PMAreaQuery // Level 6
sealed trait PMTownshipQuery             extends PMAreaQuery // Level 7
sealed trait PMDistrictQuery             extends PMAreaQuery // Level 8
sealed trait PMVillageQuery              extends PMAreaQuery // Level 9
sealed trait PMCommunityQuery            extends PMAreaQuery // Level 10
sealed trait PMPOIQuery                  extends PMQuery
sealed trait PMPOIFromAreaQuery          extends PMPOIQuery
sealed trait PMRestaurantQuery           extends PMPOIFromAreaQuery
sealed trait PMTheatreQuery              extends PMPOIFromAreaQuery
sealed trait PMSupermarketQuery          extends PMPOIFromAreaQuery
sealed trait PMMuseumQuery               extends PMPOIFromAreaQuery
sealed trait PMKindergartenQuery         extends PMPOIFromAreaQuery
sealed trait PMPublicTransportStopsQuery extends PMPOIFromAreaQuery
sealed trait PMCollegeQuery              extends PMPOIFromAreaQuery
sealed trait PMLibraryQuery              extends PMPOIFromAreaQuery
sealed trait PMSchoolQuery               extends PMPOIFromAreaQuery
sealed trait PMUniversityQuery           extends PMPOIFromAreaQuery
sealed trait PMPetrolStationQuery        extends PMPOIFromAreaQuery
sealed trait PMParkingQuery              extends PMPOIFromAreaQuery
sealed trait PMTaxiQuery                 extends PMPOIFromAreaQuery
sealed trait PMATMQuery                  extends PMPOIFromAreaQuery
sealed trait PMBankQuery                 extends PMPOIFromAreaQuery
sealed trait PMBureauDeChangeQuery       extends PMPOIFromAreaQuery
sealed trait PMClinicQuery               extends PMPOIFromAreaQuery
sealed trait PMDentistQuery              extends PMPOIFromAreaQuery
sealed trait PMDoctorQuery               extends PMPOIFromAreaQuery
sealed trait PMHospitalQuery             extends PMPOIFromAreaQuery
sealed trait PMPharmacyQuery             extends PMPOIFromAreaQuery
sealed trait PMVeterinaryQuery           extends PMPOIFromAreaQuery
sealed trait PMBrothelQuery              extends PMPOIFromAreaQuery
sealed trait PMCasinoQuery               extends PMPOIFromAreaQuery
sealed trait PMCinemaQuery               extends PMPOIFromAreaQuery
sealed trait PMNightClubQuery            extends PMPOIFromAreaQuery
sealed trait PMStripClubQuery            extends PMPOIFromAreaQuery
sealed trait PMStudioQuery               extends PMPOIFromAreaQuery
sealed trait PMCoworkingSpaceQuery       extends PMPOIFromAreaQuery
sealed trait PMFireStationQuery          extends PMPOIFromAreaQuery
sealed trait PMGymQuery                  extends PMPOIFromAreaQuery
sealed trait PMPlaceOfWorshipQuery       extends PMPOIFromAreaQuery
sealed trait PMPoliceStationQuery        extends PMPOIFromAreaQuery
sealed trait PMPostBoxQuery              extends PMPOIFromAreaQuery
sealed trait PMPostOfficeQuery           extends PMPOIFromAreaQuery
sealed trait PMPrisonQuery               extends PMPOIFromAreaQuery
sealed trait PMRecyclingContainerQuery   extends PMPOIFromAreaQuery
sealed trait PMSaunaQuery                extends PMPOIFromAreaQuery
sealed trait PMTelephoneQuery            extends PMPOIFromAreaQuery
sealed trait PMToiletQuery               extends PMPOIFromAreaQuery
sealed trait PMGolfCourseQuery           extends PMPOIFromAreaQuery
sealed trait PMIceRinkQuery              extends PMPOIFromAreaQuery
sealed trait PMParkQuery                 extends PMPOIFromAreaQuery
sealed trait PMSportPitchQuery           extends PMPOIFromAreaQuery
sealed trait PMPlaygroundQuery           extends PMPOIFromAreaQuery
sealed trait PMStadiumQuery              extends PMPOIFromAreaQuery
sealed trait PMSwimmingPoolQuery         extends PMPOIFromAreaQuery
sealed trait PMSwimmingAreaQuery         extends PMPOIFromAreaQuery
sealed trait PMSportTrackQuery           extends PMPOIFromAreaQuery
sealed trait PMBarsQuery                 extends PMPOIFromAreaQuery
sealed trait PMBarbecuesQuery            extends PMPOIFromAreaQuery
sealed trait PMBiergartensQuery          extends PMPOIFromAreaQuery
sealed trait PMCafesQuery                extends PMPOIFromAreaQuery
sealed trait PMFastFoodQuery             extends PMPOIFromAreaQuery
sealed trait PMIceCreamParloursQuery     extends PMPOIFromAreaQuery
sealed trait PMPubsQuery                 extends PMPOIFromAreaQuery
sealed trait PMTreeQuery                 extends PMPOIFromAreaQuery

sealed trait PMCoordinatesQuery extends PMQuery
final case class PMCoordinates(lon:Double, lat:Double) extends PMCoordinatesQuery

final case class PMCountryFromCoordinates(location:PMCoordinatesQuery)       extends PMCountryQuery
final case class PMCountryFromName(name:String)                           extends PMCountryQuery


final case class PMStateFromCoordinates(location:PMCoordinatesQuery)       extends PMStateQuery
final case class PMStateFromName(name:String)                           extends PMStateQuery
final case class PMStateFromArea(area:PMAreaQuery)                 extends PMStateQuery


final case class PMRegionFromCoordinates(location:PMCoordinatesQuery)       extends PMRegionQuery
final case class PMRegionFromName(name:String)                           extends PMRegionQuery
final case class PMRegionFromArea(area:PMAreaQuery)                 extends PMRegionQuery

final case class PMCityFromCoordinates(location:PMCoordinatesQuery)       extends PMCityQuery
final case class PMCityFromName(name:String)                           extends PMCityQuery
final case class PMCityFromArea(area:PMAreaQuery)                 extends PMCityQuery

final case class PMTownshipFromCoordinates(location:PMCoordinatesQuery)       extends PMTownshipQuery
final case class PMTownshipFromName(name:String)                           extends PMTownshipQuery
final case class PMTownshipFromArea(area:PMAreaQuery)                 extends PMTownshipQuery

final case class PMDistrictFromCoordinates(location:PMCoordinatesQuery)       extends PMDistrictQuery
final case class PMDistrictFromName(name:String)                           extends PMDistrictQuery
final case class PMDistrictsFromArea(area:PMAreaQuery)                 extends PMDistrictQuery

final case class PMVillageFromCoordinates(location:PMCoordinatesQuery)       extends PMVillageQuery
final case class PMVillageFromName(name:String)                           extends PMVillageQuery
final case class PMVillageFromArea(area:PMAreaQuery)                 extends PMVillageQuery

final case class PMCommunityFromCoordinates(location:PMCoordinatesQuery)       extends PMCommunityQuery
final case class PMCommunityFromName(name:String)                           extends PMCommunityQuery
final case class PMCommunityFromArea(area:PMAreaQuery)                 extends PMCommunityQuery

final case class PMBarsFromArea                 (area:PMAreaQuery) extends PMBarsQuery
final case class PMBarbecuesFromArea            (area:PMAreaQuery) extends PMBarbecuesQuery
final case class PMBiergartensFromArea          (area:PMAreaQuery) extends PMBiergartensQuery
final case class PMCafesFromArea                (area:PMAreaQuery) extends PMCafesQuery
final case class PMFastFoodFromArea             (area:PMAreaQuery) extends PMFastFoodQuery
final case class PMIceCreamParloursFromArea     (area:PMAreaQuery) extends PMIceCreamParloursQuery
final case class PMPubsFromArea                 (area:PMAreaQuery) extends PMPubsQuery
final case class PMRestaurantsFromArea          (area:PMAreaQuery) extends PMRestaurantQuery
final case class PMTheatresFromArea             (area:PMAreaQuery) extends PMTheatreQuery
final case class PMMuseumsFromArea              (area:PMAreaQuery) extends PMMuseumQuery
final case class PMSupermarketsFromArea         (area:PMAreaQuery) extends PMSupermarketQuery
final case class PMKindergartensFromArea        (area:PMAreaQuery) extends PMKindergartenQuery
final case class PMPublicTransportStopsFromArea (area:PMAreaQuery) extends PMPublicTransportStopsQuery
final case class PMCollegesFromArea             (area:PMAreaQuery) extends PMCollegeQuery
final case class PMLibrariesFromArea            (area:PMAreaQuery) extends PMLibraryQuery
final case class PMSchoolsFromArea              (area:PMAreaQuery) extends PMSchoolQuery
final case class PMUniversitiesFromArea         (area:PMAreaQuery) extends PMUniversityQuery
final case class PMPetrolStationsFromArea       (area:PMAreaQuery) extends PMPetrolStationQuery
final case class PMParkingsFromArea             (area:PMAreaQuery) extends PMParkingQuery
final case class PMTaxisFromArea                (area:PMAreaQuery) extends PMTaxiQuery
final case class PMATMsFromArea                 (area:PMAreaQuery) extends PMATMQuery
final case class PMBanksFromArea                (area:PMAreaQuery) extends PMBankQuery
final case class PMBureauxDeChangeFromArea      (area:PMAreaQuery) extends PMBureauDeChangeQuery
final case class PMClinicsFromArea              (area:PMAreaQuery) extends PMClinicQuery
final case class PMDentistsFromArea             (area:PMAreaQuery) extends PMDentistQuery
final case class PMDoctorsFromArea              (area:PMAreaQuery) extends PMDoctorQuery
final case class PMHospitalsFromArea            (area:PMAreaQuery) extends PMHospitalQuery
final case class PMPharmaciesFromArea           (area:PMAreaQuery) extends PMPharmacyQuery
final case class PMVeterinariesFromArea         (area:PMAreaQuery) extends PMVeterinaryQuery
final case class PMBrothelsFromArea             (area:PMAreaQuery) extends PMBrothelQuery
final case class PMCasinosFromArea              (area:PMAreaQuery) extends PMCasinoQuery
final case class PMCinemasFromArea              (area:PMAreaQuery) extends PMCinemaQuery
final case class PMNightClubsFromArea           (area:PMAreaQuery) extends PMNightClubQuery
final case class PMStripClubsFromArea           (area:PMAreaQuery) extends PMStripClubQuery
final case class PMStudiosFromArea              (area:PMAreaQuery) extends PMStudioQuery
final case class PMCoworkingSpacesFromArea      (area:PMAreaQuery) extends PMCoworkingSpaceQuery
final case class PMFireStationsFromArea         (area:PMAreaQuery) extends PMFireStationQuery
final case class PMGymsFromArea                 (area:PMAreaQuery) extends PMGymQuery
final case class PMPlacesOfWorshipFromArea      (area:PMAreaQuery) extends PMPlaceOfWorshipQuery
final case class PMPoliceStationsFromArea       (area:PMAreaQuery) extends PMPoliceStationQuery
final case class PMPostBoxesFromArea            (area:PMAreaQuery) extends PMPostBoxQuery
final case class PMPostOfficesFromArea          (area:PMAreaQuery) extends PMPostOfficeQuery
final case class PMPrisonsFromArea              (area:PMAreaQuery) extends PMPrisonQuery
final case class PMRecyclingContainersFromArea  (area:PMAreaQuery) extends PMRecyclingContainerQuery
final case class PMSaunasFromArea               (area:PMAreaQuery) extends PMSaunaQuery
final case class PMTelephonesFromArea           (area:PMAreaQuery) extends PMTelephoneQuery
final case class PMToiletsFromArea              (area:PMAreaQuery) extends PMToiletQuery
final case class PMGolfCoursesFromArea          (area:PMAreaQuery) extends PMGolfCourseQuery
final case class PMIceRinksFromArea             (area:PMAreaQuery) extends PMIceRinkQuery
final case class PMParksFromArea                (area:PMAreaQuery) extends PMParkQuery
final case class PMSportPitchesFromArea         (area:PMAreaQuery) extends PMSportPitchQuery
final case class PMPlaygroundsFromArea          (area:PMAreaQuery) extends PMPlaygroundQuery
final case class PMStadiumsFromArea             (area:PMAreaQuery) extends PMStadiumQuery
final case class PMSwimmingPoolsFromArea        (area:PMAreaQuery) extends PMSwimmingPoolQuery
final case class PMSwimmingAreasFromArea        (area:PMAreaQuery) extends PMSwimmingAreaQuery
final case class PMSportTracksFromArea          (area:PMAreaQuery) extends PMSportTrackQuery
final case class PMTreesFromArea          (area:PMAreaQuery) extends PMTreeQuery
