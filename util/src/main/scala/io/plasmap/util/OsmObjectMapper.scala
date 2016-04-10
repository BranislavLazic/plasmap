package io.plasmap.util

import io.plasmap.geohash._
import io.plasmap.model._
import io.plasmap.model.geometry._

/**
 * Utility class to filter too-specific tags out of a tag list and map specific tags to general purpose tags.
 */
object OsmObjectMapper {

  implicit def t(k: String, v: String): OsmTag = new OsmTag(k, v)

  private val mappings: Vector[OsmTag] = Vector(

  // AERIALWAY
  t("aerialway","cable_car"),
  t("aerialway","chair_lift"),
  t("aerialway","drag_lift"),
  t("aerialway","gondola"),
  t("aerialway","goods"),
  t("aerialway","j-bar"),
  t("aerialway","magic_carpet"),
  t("aerialway","mixed_lift"),
  t("aerialway","platter"),
  t("aerialway","pylon"),
  t("aerialway","rope_tow"),
  t("aerialway","station"),
  t("aerialway","t-bar"),
  t("aerialway","zip_line"),

  // AEROWAY
  t("aeroway","aerodrome"),
  t("aeroway","apron"),
  t("aeroway","gate"),
  t("aeroway","helipad"),
  t("aeroway","hangar"),
  t("aeroway","navigationaid"),
  t("aeroway","runway"),
  t("aeroway","taxiway"),
  t("aeroway","terminal"),
  t("aeroway","windsock"),


  // AMENITY
  // Sustenance
  t("amenity","bar"),
  t("amenity","bbq"),
  t("amenity","biergarten"),
  t("amenity","cafe"),
  t("amenity","drinking_water"),
  t("amenity","fast_food"),
  t("amenity","food_court"),
  t("amenity","ice_cream"),
  t("amenity","pub"),
  t("amenity","restaurant"),

  // Education
  t("amenity","college"),
  t("amenity","kindergarten"),
  t("amenity","library"),
  t("amenity","public_bookcase"),
  t("amenity","school"),
  t("amenity","university"),

  // Transportation
  t("amenity","bicycle_parking"),
  t("amenity","bicycle_rental"),
  t("amenity","bicycle_repair_station"),
  t("amenity","boat_sharing"),
  t("amenity","bus_station"),
  t("amenity","car_rental"),
  t("amenity","car_sharing"),
  t("amenity","car_wash"),
  t("amenity","charging_station"),
  t("amenity","ev_charging"),
  t("amenity","ferry_terminal"),
  t("amenity","fuel"),
  t("amenity","grit_bin"),
  t("amenity","motorcycle_parking"),
  t("amenity","parking"),
  t("amenity","parking_entrance"),
  t("amenity","parking_space"),
  t("amenity","taxi"),

  // Financial
  t("amenity","atm"),
  t("amenity","bank"),
  t("amenity","bureau_de_change"),

  // Healthcare
  t("amenity","baby_hatch"),
  t("amenity","clinic"),
  t("amenity","dentist"),
  t("amenity","doctors"),
  t("amenity","hospital"),
  t("amenity","nursing_home"),
  t("amenity","pharmacy"),
  t("amenity","social_facility"),
  t("amenity","veterinary"),

  // Entertainment, Arts & Culture
  t("amenity","arts_centre"),
  t("amenity","brothel"),
  t("amenity","casino"),
  t("amenity","cinema"),
  t("amenity","community_centre"),
  t("amenity","fountain"),
  t("amenity","gambling"),
  t("amenity","nightclub"),
  t("amenity","planetarium"),
  t("amenity","social_centre"),
  t("amenity","stripclub"),
  t("amenity","studio"),
  t("amenity","swingerclub"),
  t("amenity","theatre"),

  // Other
  t("amenity","animal_boarding"),
  t("amenity","animal_shelter"),
  t("amenity","bench"),
  t("amenity","clock"),
  t("amenity","courthouse"),
  t("amenity","coworking_space"),
  t("amenity","crematorium"),
  t("amenity","crypt"),
  t("amenity","dojo"),
  t("amenity","embassy"),
  t("amenity","fire_station"),
  t("amenity","game_feeding"),
  t("amenity","grave_yard"),
  t("amenity","gym"),
  t("amenity","hunting_stand"),
  t("amenity","marketplace"),
  t("amenity","photo_booth"),
  t("amenity","place_of_worship"),
  t("amenity","police"),
  t("amenity","post_box"),
  t("amenity","post_office"),
  t("amenity","prison"),
  t("amenity","ranger_station"),
  t("amenity","recycling"),
  t("amenity","register_office"),
  t("amenity","rescue_station"),
  t("amenity","sauna"),
  t("amenity","shelter"),
  t("amenity","shower"),
  t("amenity","telephone"),
  t("amenity","toilets"),
  t("amenity","townhall"),
  t("amenity","vending_machine"),
  t("amenity","waste_basket"),
  t("amenity","waste_disposal"),
  t("amenity","water_point"),
  t("amenity","watering_place"),

  // BARRIER
  // Linear
  t("barrier","cable_barrier"),
  t("barrier","city_wall"),
  t("barrier","ditch"),
  t("barrier","fence"),
  t("barrier","guard_rail"),
  t("barrier","handrail"),
  t("barrier","hedge"),
  t("barrier","kerb"),
  t("barrier","retaining_wall"),
  t("barrier","wall"),

  // Access control
  t("barrier","block"),
  t("barrier","bollard"),
  t("barrier","border_control"),
  t("barrier","bump_gate"),
  t("barrier","bus_trap"),
  t("barrier","cattle_grid"),
  t("barrier","chain"),
  t("barrier","cycle_barrier"),
  t("barrier","debris"),
  t("barrier","entrance"),
  t("barrier","full-height_turnstile"),
  t("barrier","gate"),
  t("barrier","hampshire_gate"),
  t("barrier","height_restrictor"),
  t("barrier","horse_stile"),
  t("barrier","jersey_barrier"),
  t("barrier","kent_carriage_gap"),
  t("barrier","kissing_gate"),
  t("barrier","lift_gate"),
  t("barrier","log"),
  t("barrier","motorcycle_barrier"),
  t("barrier","rope"),
  t("barrier","sally_port"),
  t("barrier","spikes"),
  t("barrier","stile"),
  t("barrier","sump_buster"),
  t("barrier","swing_gate"),
  t("barrier","toll_booth"),
  t("barrier","turnstile"),
  t("barrier","yes"),

  // BUILDING
  // Accommodation
  t("building","apartments"),
  t("building","farm"),
  t("building","hotel"),
  t("building","house"),
  t("building","detached"),
  t("building","residential"),
  t("building","dormitory"),
  t("building","terrace"),
  t("building","houseboat"),
  t("building","bungalow"),
  t("building","static_caravan"),

  // Commercial
  t("building","commercial"),
  t("building","industrial"),
  t("building","retail"),
  t("building","warehouse"),

  // Civic
  t("building","cathedral"),
  t("building","chapel"),
  t("building","church"),
  t("building","mosque"),
  t("building","temple"),
  t("building","synagogue"),
  t("building","shrine"),
  t("building","civic"),
  t("building","hospital"),
  t("building","school"),
  t("building","stadium"),
  t("building","train_station"),
  t("building","transportation"),
  t("building","university"),
  t("building","public"),

  // Other
  t("building","barn"),
  t("building","bridge"),
  t("building","bunker"),
  t("building","cabin"),
  t("building","construction"),
  t("building","cowshed"),
  t("building","farm_auxiliary"),
  t("building","garage"),
  t("building","garages"),
  t("building","greenhouse"),
  t("building","hangar"),
  t("building","hut"),
  t("building","roof"),
  t("building","shed"),
  t("building","stable"),
  t("building","sty"),
  t("building","transformer_tower"),
  t("building","service"),
  t("building","kiosk"),
  t("building","ruins"),

  // CRAFT
  t("craft","agricultural_engines"),
  t("craft","basket_maker"),
  t("craft","beekeeper"),
  t("craft","blacksmith"),
  t("craft","brewery"),
  t("craft","boatbuilder"),
  t("craft","bookbinder"),
  t("craft","carpenter"),
  t("craft","carpet_layer"),
  t("craft","caterer"),
  t("craft","clockmaker"),
  t("craft","confectionery"),
  t("craft","dressmaker"),
  t("craft","electrician"),
  t("craft","floorer"),
  t("craft","gardener"),
  t("craft","glaziery"),
  t("craft","handicraft"),
  t("craft","hvac"),
  t("craft","insulation"),
  t("craft","jeweller"),
  t("craft","key_cutter"),
  t("craft","locksmith"),
  t("craft","metal_construction"),
  t("craft","optician"),
  t("craft","painter"),
  t("craft","parquet_layer"),
  t("craft","photographer"),
  t("craft","photographic_laboratory"),
  t("craft","plasterer"),
  t("craft","plumber"),
  t("craft","pottery"),
  t("craft","rigger"),
  t("craft","roofer"),
  t("craft","saddler"),
  t("craft","sailmaker"),
  t("craft","sawmill"),
  t("craft","scaffolder"),
  t("craft","sculptor"),
  t("craft","shoemaker"),
  t("craft","stand_builder"),
  t("craft","stonemason"),
  t("craft","sun_protection"),
  t("craft","chimney_sweeper"),
  t("craft","tailor"),
  t("craft","tiler"),
  t("craft","tinsmith"),
  t("craft","upholsterer"),
  t("craft","watchmaker"),
  t("craft","window_construction"),
  t("craft","winery"),

  // EMERGENCY
  t("emergency","ambulance_station"),
  t("emergency","defibrillator"),
  t("emergency","fire_extinguisher"),
  t("emergency","fire_flapper"),
  t("emergency","fire_hose"),
  t("emergency","fire_hydrant"),
  t("emergency","water_tank"),
  t("emergency","lifeguard_base"),
  t("emergency","lifeguard_tower"),
  t("emergency","lifeguard_platform"),
  t("emergency","lifeguard_place"),
  t("emergency","life_ring"),
  t("emergency","assembly_point"),
  t("emergency","access_point"),
  t("emergency","phone"),
  t("emergency","ses_station"),
  t("emergency","siren"),

  // GEOLOGICAL
  t("geological","moraine"),
  t("geological","outcrop"),
  t("geological","palaeontological_site"),

  // HIGHWAY
  // Roads
  t("highway","motorway"),
  t("highway","trunk"),
  t("highway","primary"),
  t("highway","secondary"),
  t("highway","tertiary"),
  t("highway","unclassified"),
  t("highway","residential"),
  t("highway","service"),

  // Link roads
  t("highway","motorway_link"),
  t("highway","trunk_link"),
  t("highway","primary_link"),
  t("highway","secondary_link"),
  t("highway","tertiary_link"),

  // Special road types
  t("highway","living_street"),
  t("highway","pedestrian"),
  t("highway","track"),
  t("highway","bus_guideway"),
  t("highway","raceway"),
  t("highway","road"),

  // Other
  t("highway","footway"),
  t("highway","bridleway"),
  t("highway","steps"),
  t("highway","path"),
  t("highway","cycleway"),
  t("cycleway","lane"),
  t("cycleway","opposite"),
  t("cycleway","opposite_lane"),
  t("cycleway","track"),
  t("cycleway","opposite_track"),
  t("cycleway","share_busway"),
  t("cycleway","shared_lane"),
  t("busway","lane"),

  // HISTORIC
  t("historic","archaeological_site"),
  t("historic","aircraft"),
  t("historic","battlefield"),
  t("historic","boundary_stone"),
  t("historic","building"),
  t("historic","castle"),
  t("historic","cannon"),
  t("historic","city_gate"),
  t("historic","citywalls"),
  t("historic","farm"),
  t("historic","fort"),
  t("historic","manor"),
  t("historic","memorial"),
  t("historic","monument"),
  t("historic","optical_telegraph"),
  t("historic","ruins"),
  t("historic","rune_stone"),
  t("historic","ship"),
  t("historic","tomb"),
  t("historic","tree_shrine"),
  t("historic","wayside_cross"),
  t("historic","wayside_shrine"),
  t("historic","wreck"),

  // LANDUSE
  t("landuse","allotments"),
  t("landuse","basin"),
  t("landuse","brownfield"),
  t("landuse","cemetery"),
  t("landuse","commercial"),
  t("landuse","conservation"),
  t("landuse","construction"),
  t("landuse","farmland"),
  t("landuse","farmyard"),
  t("landuse","forest"),
  t("landuse","garages"),
  t("landuse","grass"),
  t("landuse","greenfield"),
  t("landuse","greenhouse_horticulture"),
  t("landuse","industrial"),
  t("landuse","landfill"),
  t("landuse","meadow"),
  t("landuse","military"),
  t("landuse","orchard"),
  t("landuse","pasture"),
  t("landuse","peat_cutting"),
  t("landuse","plant_nursery"),
  t("landuse","port"),
  t("landuse","quarry"),
  t("landuse","railway"),
  t("landuse","recreation_ground"),
  t("landuse","reservoir"),
  t("landuse","residential"),
  t("landuse","retail"),
  t("landuse","salt_pond"),
  t("landuse","village_green"),
  t("landuse","vineyard"),

  // LEISURE
  t("leisure","adult_gaming_centre"),
  t("leisure","amusement_arcade"),
  t("leisure","beach_resort"),
  t("leisure","bandstand"),
  t("leisure","bird_hide"),
  t("leisure","dance"),
  t("leisure","dog_park"),
  t("leisure","firepit"),
  t("leisure","fishing"),
  t("leisure","garden"),
  t("leisure","golf_course"),
  t("leisure","hackerspace"),
  t("leisure","ice_rink"),
  t("leisure","marina"),
  t("leisure","miniature_golf"),
  t("leisure","nature_reserve"),
  t("leisure","park"),
  t("leisure","pitch"),
  t("leisure","playground"),
  t("leisure","slipway"),
  t("leisure","sports_centre"),
  t("leisure","stadium"),
  t("leisure","summer_camp"),
  t("leisure","swimming_pool"),
  t("leisure","swimming_area"),
  t("leisure","track"),
  t("leisure","water_park"),
  t("leisure","wildlife_hide"),

  // MAN MADE
  t("man_made","adit"),
  t("man_made","beacon"),
  t("man_made","breakwater"),
  t("man_made","bridge"),
  t("man_made","bunker_silo"),
  t("man_made","campanile"),
  t("man_made","chimney"),
  t("man_made","communications_tower"),
  t("man_made","crane"),
  t("man_made","cross"),
  t("man_made","cutline"),
  t("man_made","clearcut"),
  t("man_made","embankment"),
  t("man_made","dyke"),
  t("man_made","flagpole"),
  t("man_made","gasometer"),
  t("man_made","groyne"),
  t("man_made","kiln"),
  t("man_made","lighthouse"),
  t("man_made","mast"),
  t("man_made","mineshaft"),
  t("man_made","monitoring_station"),
  t("man_made","offshore_platform"),
  t("man_made","petroleum_well"),
  t("man_made","pier"),
  t("man_made","pipeline"),
  t("man_made","reservoir_covered"),
  t("man_made","silo"),
  t("man_made","snow_fence"),
  t("man_made","snow_net"),
  t("man_made","storage_tank"),
  t("man_made","street_cabinet"),
  t("man_made","surveillance"),
  t("man_made","survey_point"),
  t("man_made","tower"),
  t("man_made","wastewater_plant"),
  t("man_made","watermill"),
  t("man_made","water_tower"),
  t("man_made","water_well"),
  t("man_made","water_tap"),
  t("man_made","water_works"),
  t("man_made","windmill"),
  t("man_made","works"),

  // MILITARY
  t("military","airfield"),
  t("military","ammunition"),
  t("military","bunker"),
  t("military","barracks"),
  t("military","checkpoint"),
  t("military","danger_area"),
  t("military","naval_base"),
  t("military","nuclear_explosion_site"),
  t("military","obstacle_course"),
  t("military","office"),
  t("military","range"),
  t("military","training_area"),

  // NATURAL
  // Vegetation
  t("natural","wood"),
  t("natural","tree_row"),
  t("natural","tree"),
  t("natural","scrub"),
  t("natural","heath"),
  t("natural","moor"),
  t("natural","grassland"),
  t("natural","fell"),
  t("natural","bare_rock"),
  t("natural","scree"),
  t("natural","shingle"),
  t("natural","sand"),
  t("natural","mud"),

  // Water
  t("natural","water"),
  t("natural","wetland"),
  t("natural","glacier"),
  t("natural","bay"),
  t("natural","beach"),
  t("natural","coastline"),
  t("natural","spring"),

  // Landform related
  t("natural","peak"),
  t("natural","volcano"),
  t("natural","valley"),
  t("natural","ridge"),
  t("natural","arete"),
  t("natural","cliff"),
  t("natural","saddle"),
  t("natural","rock"),
  t("natural","stone"),
  t("natural","sinkhole"),
  t("natural","cave_entrance"),

  // OFFICE
  t("office","accountant"),
  t("office","administrative"),
  t("office","advertising_agency"),
  t("office","architect"),
  t("office","association"),
  t("office","company"),
  t("office","educational_institution"),
  t("office","employment_agency"),
  t("office","estate_agent"),
  t("office","forestry"),
  t("office","foundation"),
  t("office","government"),
  t("office","guide"),
  t("office","insurance"),
  t("office","it"),
  t("office","lawyer"),
  t("office","newspaper"),
  t("office","ngo"),
  t("office","notary"),
  t("office","political_party"),
  t("office","private_investigator"),
  t("office","quango"),
  t("office","realtor"),
  t("office","register"),
  t("office","religion"),
  t("office","research"),
  t("office","tax"),
  t("office","tax_advisor"),
  t("office","telecommunication"),
  t("office","travel_agent"),
  t("office","water_utility"),

  // PLACES
  // Administrative
  t("place","country"),
  t("place","state"),
  t("place","region"),
  t("place","province"),
  t("place","district"),
  t("place","county"),
  t("place","municipality"),

  // Populated settlements, urban
  t("place","city"),
  t("place","borough"),
  t("place","suburb"),
  t("place","quarter"),
  t("place","neighbourhood"),
  t("place","city_block"),
  t("place","plot"),

  // Populated settlements, urban and rural
  t("place","town"),
  t("place","village"),
  t("place","hamlet"),
  t("place","isolated_dwelling"),
  t("place","farm"),
  t("place","allotments"),

  // Other
  t("place","continent"),
  t("place","archipelago"),
  t("place","island"),
  t("place","islet"),
  t("place","locality"),

  // POWER
  t("power","plant"),
  t("power","cable"),
  t("power","converter"),
  t("power","generator"),
  t("power","heliostat"),
  t("power","line"),
  t("power","minor_line"),
  t("power","pole"),
  t("power","substation"),
  t("power","switch"),
  t("power","tower"),
  t("power","transformer"),

  // PUBLIC TRANSPORT
  t("public_transport","stop_position"),
  t("public_transport","platform"),
  t("public_transport","station"),
  t("public_transport","stop_area"),

  // RAILWAY
  // Tracks
  t("railway","abandoned"),
  t("railway","construction"),
  t("railway","disused"),
  t("railway","funicular"),
  t("railway","light_rail"),
  t("railway","miniature"),
  t("railway","monorail"),
  t("railway","narrow_gauge"),
  t("railway","preserved"),
  t("railway","rail"),
  t("railway","subway"),
  t("railway","tram"),

  // Stations
  t("railway","halt"),
  t("railway","station"),
  t("railway","subway_entrance"),
  t("railway","tram_stop"),

  // Other
  t("railway","buffer_stop"),
  t("railway","derail"),
  t("railway","crossing"),
  t("railway","level_crossing"),
  t("landuse","railway"),
  t("railway","signal"),
  t("railway","switch"),
  t("railway","railway_crossing"),
  t("railway","turntable"),
  t("railway","roundhouse"),

  // ROUTE
  t("route","bicycle"),
  t("route","bus"),
  t("route","inline_skates"),
  t("route","canoe"),
  t("route","detour"),
  t("route","ferry"),
  t("route","hiking"),
  t("route","horse"),
  t("route","light_rail"),
  t("route","mtb"),
  t("route","nordic_walking"),
  t("route","pipeline"),
  t("route","piste"),
  t("route","power"),
  t("route","railway"),
  t("route","road"),
  t("route","running"),
  t("route","ski"),
  t("route","train"),
  t("route","tram"),

  // SHOP
  // Food, beverages
  t("shop","alcohol"),
  t("shop","bakery"),
  t("shop","beverages"),
  t("shop","butcher"),
  t("shop","cheese"),
  t("shop","chocolate"),
  t("shop","coffee"),
  t("shop","confectionery"),
  t("shop","convenience"),
  t("shop","deli"),
  t("shop","dairy"),
  t("shop","farm"),
  t("shop","greengrocer"),
  t("shop","organic"),
  t("shop","pasta"),
  t("shop","pastry"),
  t("shop","seafood"),
  t("shop","tea"),
  t("shop","wine"),

  // General store, department store, mall
  t("shop","department_store"),
  t("shop","general"),
  t("shop","kiosk"),
  t("shop","mall"),
  t("shop","supermarket"),

  // Clothing, shoes, accessories
  t("shop","baby_goods"),
  t("shop","bag"),
  t("shop","boutique"),
  t("shop","clothes"),
  t("shop","fabric"),
  t("shop","fashion"),
  t("shop","jewelry"),
  t("shop","leather"),
  t("shop","shoes"),
  t("shop","tailor"),
  t("shop","watches"),

  // Discount store, charity
  t("shop","charity"),
  t("shop","second_hand"),
  t("shop","variety_store"),

  // Health and beauty
  t("shop","beauty"),
  t("shop","chemist"),
  t("shop","cosmetics"),
  t("shop","erotic"),
  t("shop","hairdresser"),
  t("shop","hearing_aids"),
  t("shop","herbalist"),
  t("shop","massage"),
  t("shop","medical_supply"),
  t("shop","optician"),
  t("shop","perfumery"),
  t("shop","tattoo"),

  // Do-it-yourself, household, building materials, gardening
  t("shop","bathroom_furnishing"),
  t("shop","doityourself"),
  t("shop","electrical"),
  t("shop","energy"),
  t("shop","florist"),
  t("shop","furnace"),
  t("shop","garden_centre"),
  t("shop","garden_furniture"),
  t("shop","gas"),
  t("shop","glaziery"),
  t("shop","hardware"),
  t("shop","houseware"),
  t("shop","locksmith"),
  t("shop","paint"),
  t("shop","trade"),

  // Furniture and interior
  t("shop","antiques"),
  t("shop","bed"),
  t("shop","candles"),
  t("shop","carpet"),
  t("shop","curtain"),
  t("shop","furniture"),
  t("shop","interior_decoration"),
  t("shop","kitchen"),
  t("shop","window_blind"),

  // Electronics
  t("shop","computer"),
  t("shop","electronics"),
  t("shop","hifi"),
  t("shop","mobile_phone"),
  t("shop","radiotechnics"),
  t("shop","vacuum_cleaner"),

  // Outdoors and sport, vehicles
  t("shop","bicycle"),
  t("shop","car"),
  t("shop","car_repair"),
  t("shop","car_parts"),
  t("shop","fishing"),
  t("shop","free_flying"),
  t("shop","hunting"),
  t("shop","motorcycle"),
  t("shop","outdoor"),
  t("shop","scuba_diving"),
  t("shop","sports"),
  t("shop","tyres"),
  t("shop","swimming_pool"),

  // Art, music, hobbies
  t("shop","art"),
  t("shop","craft"),
  t("shop","frame"),
  t("shop","games"),
  t("shop","model"),
  t("shop","music"),
  t("shop","musical_instrument"),
  t("shop","photo"),
  t("shop","trophy"),
  t("shop","video"),
  t("shop","video_games"),

  // Stationery, gifts, books, newspapers
  t("shop","anime"),
  t("shop","books"),
  t("shop","gift"),
  t("shop","newsagent"),
  t("shop","stationery"),
  t("shop","ticket"),

  // Other
  t("shop","copyshop"),
  t("shop","dry_cleaning"),
  t("shop","e-cigarette"),
  t("shop","funeral_directors"),
  t("shop","laundry"),
  t("shop","money_lender"),
  t("shop","pawnbroker"),
  t("shop","pet"),
  t("shop","pyrotechnics"),
  t("shop","religion"),
  t("shop","storage_rental"),
  t("shop","tobacco"),
  t("shop","toys"),
  t("shop","travel_agency"),
  t("shop","vacant"),
  t("shop","weapons"),

  // TOURISM
  t("tourism","alpine_hut"),
  t("tourism","apartment"),
  t("tourism","artwork"),
  t("tourism","attraction"),
  t("tourism","camp_site"),
  t("tourism","caravan_site"),
  t("tourism","chalet"),
  t("tourism","gallery"),
  t("tourism","guest_house"),
  t("tourism","hostel"),
  t("tourism","hotel"),
  t("tourism","information"),
  t("tourism","motel"),
  t("tourism","museum"),
  t("tourism","picnic_site"),
  t("tourism","theme_park"),
  t("tourism","viewpoint"),
  t("tourism","wilderness_hut"),
  t("tourism","zoo"),

  // SPORT
  t("sport","10pin"),
  t("sport","9pin"),
  t("sport","aikido"),
  t("sport","american_football"),
  t("sport","archery"),
  t("sport","athletics"),
  t("sport","australian_football"),
  t("sport","badminton"),
  t("sport","bandy"),
  t("sport","base"),
  t("sport","baseball"),
  t("sport","basketball"),
  t("sport","beachvolleyball"),
  t("sport","billiards"),
  t("sport","bmx"),
  t("sport","bobsleigh"),
  t("sport","boules"),
  t("sport","bowls"),
  t("sport","boxing"),
  t("sport","canadian_football"),
  t("sport","canoe"),
  t("sport","chess"),
  t("sport","cliff_diving"),
  t("sport","climbing"),
  t("sport","climbing_adventure"),
  t("sport","cockfighting"),
  t("sport","cricket"),
  t("sport","cricket_nets"),
  t("sport","croquet"),
  t("sport","curling"),
  t("sport","cycling"),
  t("sport","darts"),
  t("sport","diving"),
  t("sport","dog_racing"),
  t("sport","equestrian"),
  t("sport","fencing"),
  t("sport","field_hockey"),
  t("sport","football"),
  t("sport","free_flying"),
  t("sport","gaelic_games"),
  t("sport","golf"),
  t("sport","gymnastics"),
  t("sport","handball"),
  t("sport","hapkido"),
  t("sport","hockey"),
  t("sport","horse_racing"),
  t("sport","horseshoes"),
  t("sport","ice_hockey"),
  t("sport","ice_skating"),
  t("sport","ice_stock"),
  t("sport","judo"),
  t("sport","karting"),
  t("sport","kitesurfing"),
  t("sport","korfball"),
  t("sport","model_aerodrome"),
  t("sport","motocross"),
  t("sport","motor"),
  t("sport","multi"),
  t("sport","obstacle_course"),
  t("sport","orienteering"),
  t("sport","paddle_tennis"),
  t("sport","paragliding"),
  t("sport","pelota"),
  t("sport","racquet"),
  t("sport","rc_car"),
  t("sport","roller_skating"),
  t("sport","rowing"),
  t("sport","rugby_league"),
  t("sport","rugby_union"),
  t("sport","running"),
  t("sport","safety_training"),
  t("sport","sailing"),
  t("sport","scuba_diving"),
  t("sport","shooting"),
  t("sport","skateboard"),
  t("sport","skating"),
  t("sport","skiing"),
  t("sport","soccer"),
  t("sport","surfing"),
  t("sport","swimming"),
  t("sport","table_soccer"),
  t("sport","table_tennis"),
  t("sport","taekwondo"),
  t("sport","tennis"),
  t("sport","toboggan"),
  t("sport","volleyball"),
  t("sport","water_polo"),
  t("sport","water_ski"),
  t("sport","weightlifting"),
  t("sport","wrestling"),


  // WATERWAY
  // Natural
  t("waterway","river"),
  t("waterway","riverbank"),
  t("waterway","stream"),
  t("waterway","brook"),

  // Man made
  t("waterway","canal"),
  t("waterway","drain"),
  t("waterway","ditch"),

  // Facilities
  t("waterway","dock"),
  t("waterway","boatyard"),

  // Barriers
  t("waterway","dam"),
  t("waterway","weir"),
  t("waterway","waterfall"),
  t("waterway","lock_gate"),

  // Other
  t("waterway","turning_point"),
  t("waterway","water_point"),

  t("boundary", "administrative"),
  t("admin_level", "1"),
  t("admin_level", "2"),
  t("admin_level", "3"),
  t("admin_level", "4"),
  t("admin_level", "5"),
  t("admin_level", "6"),
  t("admin_level", "7"),
  t("admin_level", "8"),
  t("admin_level", "9"),
  t("admin_level", "10"),
  t("admin_level", "11")
  )

  val ultraLow = GeoHash.ultraLow
  val veryLow = GeoHash.veryLow
  val low = GeoHash.low
  val medium = GeoHash.medium
  val high = GeoHash.high
  val veryHigh = GeoHash.veryHigh
  val ultra = GeoHash.ultra
  val ultraHigh = GeoHash.ultraHigh

  def tags(osmObject: OsmObject): List[OsmTag] = extractTags(osmObject.tags)

  def tags(osmObject: OsmDenormalizedObject): List[OsmTag] = extractTags(osmObject.tags)


  private[this] def extractTags(tags: List[OsmTag]): List[OsmTag] = {
    tags.intersect(mappings).distinct
  }


  def hasher(osmObject: OsmDenormalizedObject): GeoHash = osmObject match {
    case node: OsmDenormalizedNode => low
    case way: OsmDenormalizedWay => low
    case relation: OsmDenormalizedRelation => veryLow
  }

  def hasher(tags: List[OsmTag]): GeoHash =
    if (tags.contains(OsmTag("administrative", "boundary"))
      && tags.contains(OsmTag("admin_level", "8"))) {
      veryLow
    } else {
      low
    }


  def principalBoundingBox(osmObject: OsmDenormalizedObject): Long = osmObject match {
    case node@OsmDenormalizedNode(id, user, version, tags, point) =>
      low.encodeParallel(point.lon, point.lat)

    case way@OsmDenormalizedWay(id, user, version, tags, linestring) =>
      val bbs = calculateWayBoundingBoxes(hasher(way), linestring).toList
      bbs.head

    case relation@OsmDenormalizedRelation(id, user, version, tags, geometryCollection) =>
      val bbs = calculateRelationBoundingBoxes(hasher(relation), geometryCollection).toList
      bbs.head


  }

  def boundingBoxes(osmObject: OsmDenormalizedObject): Set[Long] = {

    val bbHasher = hasher(osmObject)
    val geometry: Geometry = osmObject.geometry
    calculateBoundingBoxes(bbHasher, geometry)
  }

  def calculateBoundingBoxes(bbHasher: GeoHash, geometry: Geometry): Set[Long] = geometry match {
    case node: Point => calculateNodeBoundingBoxes(bbHasher, node)
    case way: LineString => calculateWayBoundingBoxes(bbHasher, way)
    case relation: GeometryCollection => calculateRelationBoundingBoxes(bbHasher, relation)
    case mp: MultiPoint => calculateCoordinatesBoundingBoxes(bbHasher, mp.coordinates)
    case mls: MultiLineString => calculateCoordinatesBoundingBoxes(bbHasher, mls.coordinates.flatten)
    case poly: Polygon => calculateCoordinatesBoundingBoxes(bbHasher, poly.coordinates.flatten)
    case mp: MultiPolygon => mp.coordinates.map(Polygon).flatMap(calculateBoundingBoxes(bbHasher, _)).toSet
  }

  def calculateWayBoundingBoxes(bbHasher: GeoHash, linestring: LineString): Set[Long] = {
    val coordinates: List[(Double, Double)] = linestring.coordinates
    calculateCoordinatesBoundingBoxes(bbHasher, coordinates)
  }

  private def calculateCoordinatesBoundingBoxes(bbHasher: GeoHash, coordinates: List[(Double, Double)]): Set[Long] = {
    (for {
      (lon, lat) <- coordinates
    } yield bbHasher.encodeParallel(lon, lat)).toSet
  }

  def calculateNodeBoundingBoxes(bbHasher: GeoHash, point: Point): Set[Long] =
    Set(bbHasher.encodeParallel(point.lon, point.lat))

  def calculateRelationBoundingBoxes(bbHasher: GeoHash, geometry: GeometryCollection): Set[Long] = {
    val boundingBoxes = for {
      geo ← geometry.flatten
      bb ← calculateBoundingBoxes(bbHasher, geo)
    } yield bb
    boundingBoxes.toSet
  }

}
