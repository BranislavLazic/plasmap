package io.plasmap.js

import io.plasmap.pamphlet._
import io.plasmap.querymodel._
import scala.scalajs.js
import scala.scalajs.js.{JSON, UndefOr}
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import org.scalajs.dom._
import scalaz.syntax.id._

@JSExport("Plasmap")
@JSExportAll
object PlasmapJS {
  withGlobals().withLocation()

  def withLocation():PlasmapJS.type = {
    org.scalajs.dom.navigator.geolocation.getCurrentPosition((pos:Position) =>
      {
        scalajs.js.Dynamic.global.geolocation = PMCoordinates(pos.coords.longitude, pos.coords.latitude).asInstanceOf[js.Any]
        ()
      }, (err:PositionError) => println("Could not get geolocation.")
    )
    this
  }

  def withGlobals():PlasmapJS.type = {

    scalajs.js.Dynamic.global.coordinates          = { (lon:Double, lat:Double) => coordinates(lon, lat) }

    scalajs.js.Dynamic.global.country = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => country(lat, lon)
        case (name: String, _, _)               => country(name)
        case (coords:PMCoordinates, _, _)       => country(coords)
        case _ ⇒ js.undefined:js.UndefOr[PMCountryQuery]
      }
    }

    scalajs.js.Dynamic.global.state = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => state(lat, lon)
        case (name: String, _, _)               => state(name)
        case (coords:PMCoordinates, _, _)       => state(coords)
        case (area:PMAreaQuery,_,_)             => state(area)
        case _ ⇒ js.undefined:js.UndefOr[PMStateQuery]
      }
    }

    scalajs.js.Dynamic.global.region = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => region(lat, lon)
        case (name: String, _, _)               => region(name)
        case (coords:PMCoordinates, _, _)       => region(coords)
        case (area:PMAreaQuery,_,_)             => region(area)
        case _ ⇒ js.undefined:js.UndefOr[PMRegionQuery]
      }
    }

    scalajs.js.Dynamic.global.city = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => city(lat, lon)
        case (name: String, _, _)               => city(name)
        case (coords:PMCoordinates, _, _)       => city(coords)
        case (area:PMAreaQuery,_,_)             => city(area)
        case _ ⇒ js.undefined:js.UndefOr[PMCityQuery]
      }
    }

    scalajs.js.Dynamic.global.township = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => township(lat, lon)
        case (name: String, _, _)               => township(name)
        case (coords:PMCoordinates, _, _)       => township(coords)
        case (area:PMAreaQuery,_,_)             => township(area)
        case _ ⇒ js.undefined:js.UndefOr[PMTownshipQuery]
      }
    }

    scalajs.js.Dynamic.global.district            = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => district(lat, lon)
        case (name: String, _, _)               => district(name)
        case (coords:PMCoordinates, _, _)       => district(coords)
        case (area:PMAreaQuery,_,_)             => district(area)
        case _ ⇒ js.undefined:js.UndefOr[PMDistrictQuery]
      }
    }

    scalajs.js.Dynamic.global.village = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => village(lat, lon)
        case (name: String, _, _)               => village(name)
        case (coords:PMCoordinates, _, _)       => village(coords)
        case (area:PMAreaQuery,_,_)             => village(area)
        case _ ⇒ js.undefined:js.UndefOr[PMVillageQuery]
      }
    }

    scalajs.js.Dynamic.global.community = { (arg1: Any, arg2: Any, arg3: Any) =>
      (arg1, arg2, arg3) match {
        case (lat: Double, lon: Double, _:Unit) => community(lat, lon)
        case (name: String, _, _)               => community(name)
        case (coords:PMCoordinates, _, _)       => community(coords)
        case (area:PMAreaQuery,_,_)             => community(area)
        case _ ⇒ js.undefined:js.UndefOr[PMCommunityQuery]
      }
    }
    //JSOnly

    scalajs.js.Dynamic.global.restaurants          = { (area:PMAreaQuery) => restaurants(area) }
    scalajs.js.Dynamic.global.theatres             = { (area:PMAreaQuery) => theatres(area)     }
    scalajs.js.Dynamic.global.supermarkets         = { (area:PMAreaQuery) => supermarkets(area) }
    scalajs.js.Dynamic.global.museums              = { (area:PMAreaQuery) => museums(area) }
    scalajs.js.Dynamic.global.kindergartens        = { (area:PMAreaQuery) => kindergartens(area) }
    scalajs.js.Dynamic.global.publicTransportStops = { (area:PMAreaQuery) => publicTransportStops(area) }
    scalajs.js.Dynamic.global.colleges             = { (area:PMAreaQuery) => colleges(area) }
    scalajs.js.Dynamic.global.libraries            = { (area:PMAreaQuery) => libraries(area) }
    scalajs.js.Dynamic.global.schools              = { (area:PMAreaQuery) => schools(area) }
    scalajs.js.Dynamic.global.universities         = { (area:PMAreaQuery) => universities(area) }
    scalajs.js.Dynamic.global.petrolStations       = { (area:PMAreaQuery) => petrolStations(area) }
    scalajs.js.Dynamic.global.parkings             = { (area:PMAreaQuery) => parkings(area) }
    scalajs.js.Dynamic.global.taxis                = { (area:PMAreaQuery) => taxis(area) }
    scalajs.js.Dynamic.global.atms                 = { (area:PMAreaQuery) => atms(area) }
    scalajs.js.Dynamic.global.banks                = { (area:PMAreaQuery) => banks(area) }
    scalajs.js.Dynamic.global.bureauxDeChange      = { (area:PMAreaQuery) => bureauxDeChange(area) }
    scalajs.js.Dynamic.global.clinics              = { (area:PMAreaQuery) => clinics(area) }
    scalajs.js.Dynamic.global.dentists             = { (area:PMAreaQuery) => dentists(area) }
    scalajs.js.Dynamic.global.doctors              = { (area:PMAreaQuery) => doctors(area) }
    scalajs.js.Dynamic.global.hospitals            = { (area:PMAreaQuery) => hospitals(area) }
    scalajs.js.Dynamic.global.pharmacies           = { (area:PMAreaQuery) => pharmacies(area) }
    scalajs.js.Dynamic.global.veterinaries         = { (area:PMAreaQuery) => veterinaries(area) }
    scalajs.js.Dynamic.global.brothels             = { (area:PMAreaQuery) => brothels(area) }
    scalajs.js.Dynamic.global.casinos              = { (area:PMAreaQuery) => casinos(area) }
    scalajs.js.Dynamic.global.cinemas              = { (area:PMAreaQuery) => cinemas(area) }
    scalajs.js.Dynamic.global.nightClubs           = { (area:PMAreaQuery) => nightClubs(area) }
    scalajs.js.Dynamic.global.stripClubs           = { (area:PMAreaQuery) => stripClubs(area) }
    scalajs.js.Dynamic.global.studio               = { (area:PMAreaQuery) => studio(area) }
    scalajs.js.Dynamic.global.coworkingSpaces      = { (area:PMAreaQuery) => coworkingSpaces(area) }
    scalajs.js.Dynamic.global.fireStations         = { (area:PMAreaQuery) => fireStations(area) }
    scalajs.js.Dynamic.global.gyms                 = { (area:PMAreaQuery) => gyms(area) }
    scalajs.js.Dynamic.global.placesOfWorship      = { (area:PMAreaQuery) => placesOfWorship(area) }
    scalajs.js.Dynamic.global.policeStations       = { (area:PMAreaQuery) => policeStations(area) }
    scalajs.js.Dynamic.global.postBoxes            = { (area:PMAreaQuery) => postBoxes(area) }
    scalajs.js.Dynamic.global.postOffices          = { (area:PMAreaQuery) => postOffices(area) }
    scalajs.js.Dynamic.global.prisons              = { (area:PMAreaQuery) => prisons(area) }
    scalajs.js.Dynamic.global.recyclingContainers  = { (area:PMAreaQuery) => recyclingContainers(area) }
    scalajs.js.Dynamic.global.saunas               = { (area:PMAreaQuery) => saunas(area) }
    scalajs.js.Dynamic.global.telephones           = { (area:PMAreaQuery) => telephones(area) }
    scalajs.js.Dynamic.global.toilets              = { (area:PMAreaQuery) => toilets(area) }
    scalajs.js.Dynamic.global.golfCourses          = { (area:PMAreaQuery) => golfCourses(area) }
    scalajs.js.Dynamic.global.iceRinks             = { (area:PMAreaQuery) => iceRinks(area) }
    scalajs.js.Dynamic.global.parks                = { (area:PMAreaQuery) => parks(area) }
    scalajs.js.Dynamic.global.sportPitches         = { (area:PMAreaQuery) => sportPitches(area) }
    scalajs.js.Dynamic.global.playgrounds          = { (area:PMAreaQuery) => playgrounds(area) }
    scalajs.js.Dynamic.global.stadiums             = { (area:PMAreaQuery) => stadiums(area) }
    scalajs.js.Dynamic.global.swimmingPools        = { (area:PMAreaQuery) => swimmingPools(area) }
    scalajs.js.Dynamic.global.swimmingAreas        = { (area:PMAreaQuery) => swimmingAreas(area) }
    scalajs.js.Dynamic.global.sportTracks          = { (area:PMAreaQuery) => sportTracks(area) }
    scalajs.js.Dynamic.global.bars                 = { (area:PMAreaQuery) => bars(area) }
    scalajs.js.Dynamic.global.barbecues            = { (area:PMAreaQuery) => barbecues(area) }
    scalajs.js.Dynamic.global.biergartens          = { (area:PMAreaQuery) => biergartens(area) }
    scalajs.js.Dynamic.global.cafes                = { (area:PMAreaQuery) => cafes(area) }
    scalajs.js.Dynamic.global.fastFood             = { (area:PMAreaQuery) => fastFood(area) }
    scalajs.js.Dynamic.global.iceCreamParlours     = { (area:PMAreaQuery) => iceCreamParlours(area) }
    scalajs.js.Dynamic.global.pubs                 = { (area:PMAreaQuery) => pubs(area) }
    scalajs.js.Dynamic.global.trees                = { (area:PMAreaQuery) => trees(area) }

    this
  }

  def coordinates(lon:Double, lat:Double) = PMCoordinates(lon, lat)

  def country(lon: Double, lat:Double):PMCountryFromCoordinates = PMCountryFromCoordinates(PMCoordinates(lon, lat))
  def country(coords:PMCoordinates):PMCountryFromCoordinates = PMCountryFromCoordinates(coords)
  def country(name:String):PMCountryFromName = PMCountryFromName(name)

  def state(lon: Double, lat:Double):PMStateFromCoordinates = PMStateFromCoordinates(PMCoordinates(lon, lat))
  def state(coords:PMCoordinates):PMStateFromCoordinates = PMStateFromCoordinates(coords)
  def state(name:String):PMStateFromName = PMStateFromName(name)
  def state(area:PMAreaQuery) = PMStateFromArea(area)

  def region(lon: Double, lat:Double):PMRegionFromCoordinates = PMRegionFromCoordinates(PMCoordinates(lon, lat))
  def region(coords:PMCoordinates):PMRegionFromCoordinates = PMRegionFromCoordinates(coords)
  def region(name:String):PMRegionFromName = PMRegionFromName(name)
  def region(area:PMAreaQuery) = PMRegionFromArea(area)

  def city(lon: Double, lat:Double):PMCityFromCoordinates = PMCityFromCoordinates(PMCoordinates(lon, lat))
  def city(coords:PMCoordinates):PMCityFromCoordinates = PMCityFromCoordinates(coords)
  def city(name:String):PMCityFromName = PMCityFromName(name)
  def city(area:PMAreaQuery) = PMCityFromArea(area)

  def township(lon: Double, lat:Double):PMTownshipFromCoordinates = PMTownshipFromCoordinates(PMCoordinates(lon, lat))
  def township(coords:PMCoordinates):PMTownshipFromCoordinates = PMTownshipFromCoordinates(coords)
  def township(name:String):PMTownshipFromName = PMTownshipFromName(name)
  def township(area:PMAreaQuery) = PMTownshipFromArea(area)

  def district(lon: Double, lat:Double):PMDistrictFromCoordinates = PMDistrictFromCoordinates(PMCoordinates(lon, lat))
  def district(coords:PMCoordinates):PMDistrictFromCoordinates = PMDistrictFromCoordinates(coords)
  def district(name:String):PMDistrictQuery = PMDistrictFromName(name)
  def district(area:PMAreaQuery) = PMDistrictsFromArea(area)

  def village(lon: Double, lat:Double):PMVillageFromCoordinates = PMVillageFromCoordinates(PMCoordinates(lon, lat))
  def village(coords:PMCoordinates):PMVillageFromCoordinates = PMVillageFromCoordinates(coords)
  def village(name:String):PMVillageFromName = PMVillageFromName(name)
  def village(area:PMAreaQuery) = PMVillageFromArea(area)

  def community(lon: Double, lat:Double):PMCommunityFromCoordinates = PMCommunityFromCoordinates(PMCoordinates(lon, lat))
  def community(coords:PMCoordinates):PMCommunityFromCoordinates = PMCommunityFromCoordinates(coords)
  def community(name:String):PMCommunityQuery = PMCommunityFromName(name)
  def community(area:PMAreaQuery) = PMCommunityFromArea(area)

  def bars                 (area:PMAreaQuery) = PMBarsFromArea                 (area)
  def barbecues            (area:PMAreaQuery) = PMBarbecuesFromArea            (area)
  def biergartens          (area:PMAreaQuery) = PMBiergartensFromArea          (area)
  def cafes                (area:PMAreaQuery) = PMCafesFromArea                (area)
  def fastFood             (area:PMAreaQuery) = PMFastFoodFromArea             (area)
  def iceCreamParlours     (area:PMAreaQuery) = PMIceCreamParloursFromArea     (area)
  def pubs                 (area:PMAreaQuery) = PMPubsFromArea                 (area)
  def restaurants          (area:PMAreaQuery) = PMRestaurantsFromArea          (area)
  def theatres             (area:PMAreaQuery) = PMTheatresFromArea             (area)
  def museums              (area:PMAreaQuery) = PMMuseumsFromArea              (area)
  def supermarkets         (area:PMAreaQuery) = PMSupermarketsFromArea         (area)
  def kindergartens        (area:PMAreaQuery) = PMKindergartensFromArea        (area)
  def publicTransportStops (area:PMAreaQuery) = PMPublicTransportStopsFromArea (area)
  def colleges             (area:PMAreaQuery) = PMCollegesFromArea             (area)
  def libraries            (area:PMAreaQuery) = PMLibrariesFromArea            (area)
  def schools              (area:PMAreaQuery) = PMSchoolsFromArea              (area)
  def universities         (area:PMAreaQuery) = PMUniversitiesFromArea         (area)
  def petrolStations       (area:PMAreaQuery) = PMPetrolStationsFromArea       (area)
  def parkings             (area:PMAreaQuery) = PMParkingsFromArea             (area)
  def taxis                (area:PMAreaQuery) = PMTaxisFromArea                (area)
  def atms                 (area:PMAreaQuery) = PMATMsFromArea                 (area)
  def banks                (area:PMAreaQuery) = PMBanksFromArea                (area)
  def bureauxDeChange      (area:PMAreaQuery) = PMBureauxDeChangeFromArea      (area)
  def clinics              (area:PMAreaQuery) = PMClinicsFromArea              (area)
  def dentists             (area:PMAreaQuery) = PMDentistsFromArea             (area)
  def doctors              (area:PMAreaQuery) = PMDoctorsFromArea              (area)
  def hospitals            (area:PMAreaQuery) = PMHospitalsFromArea            (area)
  def pharmacies           (area:PMAreaQuery) = PMPharmaciesFromArea           (area)
  def veterinaries         (area:PMAreaQuery) = PMVeterinariesFromArea         (area)
  def brothels             (area:PMAreaQuery) = PMBrothelsFromArea             (area)
  def casinos              (area:PMAreaQuery) = PMCasinosFromArea              (area)
  def cinemas              (area:PMAreaQuery) = PMCinemasFromArea              (area)
  def nightClubs           (area:PMAreaQuery) = PMNightClubsFromArea           (area)
  def stripClubs           (area:PMAreaQuery) = PMStripClubsFromArea           (area)
  def studio               (area:PMAreaQuery) = PMStudiosFromArea              (area)
  def coworkingSpaces      (area:PMAreaQuery) = PMCoworkingSpacesFromArea      (area)
  def fireStations         (area:PMAreaQuery) = PMFireStationsFromArea         (area)
  def gyms                 (area:PMAreaQuery) = PMGymsFromArea                 (area)
  def placesOfWorship      (area:PMAreaQuery) = PMPlacesOfWorshipFromArea      (area)
  def policeStations       (area:PMAreaQuery) = PMPoliceStationsFromArea       (area)
  def postBoxes            (area:PMAreaQuery) = PMPostBoxesFromArea            (area)
  def postOffices          (area:PMAreaQuery) = PMPostOfficesFromArea          (area)
  def prisons              (area:PMAreaQuery) = PMPrisonsFromArea              (area)
  def recyclingContainers  (area:PMAreaQuery) = PMRecyclingContainersFromArea  (area)
  def saunas               (area:PMAreaQuery) = PMSaunasFromArea               (area)
  def telephones           (area:PMAreaQuery) = PMTelephonesFromArea           (area)
  def toilets              (area:PMAreaQuery) = PMToiletsFromArea              (area)
  def golfCourses          (area:PMAreaQuery) = PMGolfCoursesFromArea          (area)
  def iceRinks             (area:PMAreaQuery) = PMIceRinksFromArea             (area)
  def parks                (area:PMAreaQuery) = PMParksFromArea                (area)
  def sportPitches         (area:PMAreaQuery) = PMSportPitchesFromArea         (area)
  def playgrounds          (area:PMAreaQuery) = PMPlaygroundsFromArea          (area)
  def stadiums             (area:PMAreaQuery) = PMStadiumsFromArea             (area)
  def swimmingPools        (area:PMAreaQuery) = PMSwimmingPoolsFromArea        (area)
  def swimmingAreas        (area:PMAreaQuery) = PMSwimmingAreasFromArea        (area)
  def sportTracks          (area:PMAreaQuery) = PMSportTracksFromArea          (area)
  def trees                (area:PMAreaQuery) = PMTreesFromArea          (area)

  def executeQueryAt(address:String, query:PMQuery) =
    ExecutorService.postWithAddress(address, query)

  def getWebSocketInternal(address:String,
                   onMessage:js.Object => Unit,
                   onComplete: (js.Array[js.Object]) => Unit) = {
    PlasmapSocket(s"$address", _.fold(println, onMessage), onComplete)
  }

  def getDefaultMap = {
    println("Setting up default plasmap map.")
    val uri = s"http://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
    val attribution =
      """
        | Map tiles by CartoDB, under CC BY 3.0. Data by OpenStreetMap, under ODbL.
        | Queries hyper-charged by <a href="http://plasmap.io">plasmap</a>""".stripMargin

    val tileLayerOptions = TileLayerOptions
      .attribution(attribution)
      .detectRetina(true)
      .build

    val tileLayer = Leaflet.tileLayer(uri, tileLayerOptions)
    val mapOptions = LeafletMapOptions
      .zoomControl(false)
      .scrollWheelZoom(true)
      .build
    val theMap = Leaflet.map("plasmap", mapOptions)
    tileLayer.addTo(theMap)
    theMap
  }

  var bounds:UndefOr[LatLngBounds] = js.undefined
  lazy val defaultMap = getDefaultMap

  type OnMessageJS = js.Function1[js.Object, Unit]
  type OnMessage = (js.Object) => Unit

  type OnCompleteJS = js.Function1[js.Array[js.Object], Unit]
  type OnComplete = (js.Array[js.Object]) => Unit

  val defaultAddress = "ws://adam.plasmap.io:9000/api/websocket"

  def channelWithDefaultMap(onMessage:UndefOr[OnMessageJS], onComplete:UndefOr[OnCompleteJS]): PlasmapSocket =
    socketForMap(defaultMap, onMessage, onComplete)

  def runInternal(m:LeafletMap)(q:PMQuery, onMessage:UndefOr[OnMessageJS], onComplete:UndefOr[OnMessageJS]):Unit = {
    val webSocket:PlasmapSocket = socketForMap(m, onMessage, onComplete)
    def coordinatesToGeoJSON(lon:Double, lat:Double):js.Object = {
      JSON.parse(s"""{ "type":"Feature", "geometry": { "type": "Point", "coordinates:[$lon, $lat]" } }""").asInstanceOf[js.Object]
    }
    webSocket.sendMessage(q)
  }

  def runOnMap(m:LeafletMap) = runInternal(m) _:js.Function3[PMQuery, UndefOr[OnMessageJS], UndefOr[OnMessageJS], Unit]

  val run =
    runInternal(defaultMap) _:js.Function3[PMQuery, UndefOr[OnMessageJS], UndefOr[OnMessageJS], Unit]

  def channel(onMessage:OnMessageJS, onComplete:OnCompleteJS):PlasmapSocket =
    getWebSocketInternal(defaultAddress, onMessage, onComplete)

  val defaultHandler:LeafletMap => OnMessage = (map:LeafletMap) => (obj:js.Object) => {

    val iconOptions = IconOptions
      .iconUrl("marker.png")
      .iconRetinaUrl("marker2x.png")
      .iconAnchor(Leaflet.point(12, 12))
      .popupAnchor(Leaflet.point(0, -32))
      .iconSize(Leaflet.point(24, 24))
      .build

    def markerOptions = MarkerOptions.icon(Leaflet.icon(iconOptions)).build
    val layerOpts = GeoJsonLayerOptions
      .fill(true)
      .fillColor("#bb0055")
      .fillOpacity(0.3)
      .color("#bb0055")
      .weight(1)
      .pointToLayer((_:js.Object, latLng:LeafletLatLng) => Leaflet.marker(latLng, markerOptions))
      .clickable(false)
      .build


    val geoJsonLayer = Leaflet.geoJson(obj, layerOpts)
    map.addLayer(geoJsonLayer)
    val gjBounds = geoJsonLayer.getBounds()
    if(!bounds.isDefined) bounds = gjBounds
    bounds = bounds.map(_.extend(gjBounds))
    map.fitBounds(bounds.get)
    ()
  }

  def socketForMap(map:LeafletMap, onMessage:UndefOr[OnMessageJS], onComplete:UndefOr[OnCompleteJS]): PlasmapSocket = {
    println("Making socket")

    val cb = (obj:js.Object) => {
      if(map == defaultMap) defaultHandler(map)(obj)
      onMessage.foreach(_(obj))
    }

    val complete = onComplete.map(x => x:OnComplete).getOrElse((_:js.Array[js.Object]) => ())

    getWebSocketInternal(defaultAddress, cb, complete)
  }
}
