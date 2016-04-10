package io.plasmap.query.engine

import io.plasmap.model.geometry.Point
import io.plasmap.model.{OsmDenormalizedObject, OsmDenormalizedRelation, OsmId, OsmTag}
import io.plasmap.query.engine.TypeAliases.SourceGraph
import simulacrum.typeclass

sealed trait Element

sealed trait OsmObjectElement extends Element {
  def osmObject:OsmDenormalizedObject
  override def toString = s"${getClass.getName}-${osmObject.id}"
}

sealed trait AreaElement extends OsmObjectElement {
  def osmObject:OsmDenormalizedRelation
}

/*
sealed trait PMCountryQuery              extends PMAreaQuery // Level 2
sealed trait PMStateQuery                extends PMAreaQuery // Level 4
sealed trait PMRegionQuery               extends PMAreaQuery // Level 5
sealed trait PMCityQuery                 extends PMAreaQuery // Level 6
sealed trait PMTownshipQuery             extends PMAreaQuery // Level 7
sealed trait PMDistrictQuery             extends PMAreaQuery // Level 8
sealed trait PMVillageQuery              extends PMAreaQuery // Level 9
sealed trait PMCommunityQuery            extends PMAreaQuery // Level 10
*/
case class Country(osmObject:OsmDenormalizedRelation) extends AreaElement
case class State(osmObject:OsmDenormalizedRelation) extends AreaElement
case class Region(osmObject:OsmDenormalizedRelation) extends AreaElement
case class City(osmObject:OsmDenormalizedRelation) extends AreaElement
case class Township(osmObject:OsmDenormalizedRelation) extends AreaElement
case class District(osmObject:OsmDenormalizedRelation) extends AreaElement
case class Village(osmObject:OsmDenormalizedRelation) extends AreaElement
case class Community(osmObject:OsmDenormalizedRelation) extends AreaElement

sealed trait POIElement extends Element
final case class Restaurant(osmObject:OsmDenormalizedObject)                  extends POIElement
final case class Theatre(osmObject:OsmDenormalizedObject)                     extends POIElement
final case class Supermarket(osmObject:OsmDenormalizedObject)                 extends POIElement
final case class Museum(osmObject:OsmDenormalizedObject)                      extends POIElement
final case class Kindergarten(osmObject:OsmDenormalizedObject)                extends POIElement
final case class PublicTransportStopPosition(osmObject:OsmDenormalizedObject) extends POIElement

final case class Bar(osmObject:OsmDenormalizedObject)                extends POIElement
final case class Barbecue(osmObject:OsmDenormalizedObject)           extends POIElement
final case class Biergarten(osmObject:OsmDenormalizedObject)         extends POIElement
final case class Cafe(osmObject:OsmDenormalizedObject)               extends POIElement
final case class FastFood(osmObject:OsmDenormalizedObject)           extends POIElement
final case class IceCreamParlour(osmObject:OsmDenormalizedObject)    extends POIElement
final case class Pub(osmObject:OsmDenormalizedObject)                extends POIElement
final case class College(osmObject:OsmDenormalizedObject)            extends POIElement
final case class Library(osmObject:OsmDenormalizedObject)            extends POIElement
final case class School(osmObject:OsmDenormalizedObject)             extends POIElement
final case class University(osmObject:OsmDenormalizedObject)         extends POIElement
final case class PetrolStation(osmObject:OsmDenormalizedObject)      extends POIElement
final case class Parking(osmObject:OsmDenormalizedObject)            extends POIElement
final case class Taxi(osmObject:OsmDenormalizedObject)               extends POIElement
final case class ATM(osmObject:OsmDenormalizedObject)                extends POIElement
final case class Bank(osmObject:OsmDenormalizedObject)               extends POIElement
final case class BureauDeChange(osmObject:OsmDenormalizedObject)     extends POIElement
final case class Clinic(osmObject:OsmDenormalizedObject)             extends POIElement
final case class Dentist(osmObject:OsmDenormalizedObject)            extends POIElement
final case class Doctor(osmObject:OsmDenormalizedObject)             extends POIElement
final case class Hospital(osmObject:OsmDenormalizedObject)           extends POIElement
final case class Pharmacy(osmObject:OsmDenormalizedObject)           extends POIElement
final case class Veterinary(osmObject:OsmDenormalizedObject)         extends POIElement
final case class Brothel(osmObject:OsmDenormalizedObject)            extends POIElement
final case class Casino(osmObject:OsmDenormalizedObject)             extends POIElement
final case class Cinema(osmObject:OsmDenormalizedObject)             extends POIElement
final case class NightClub(osmObject:OsmDenormalizedObject)          extends POIElement
final case class StripClub(osmObject:OsmDenormalizedObject)          extends POIElement
final case class Studio(osmObject:OsmDenormalizedObject)             extends POIElement
final case class CoworkingSpace(osmObject:OsmDenormalizedObject)     extends POIElement
final case class FireStation(osmObject:OsmDenormalizedObject)        extends POIElement
final case class Gym(osmObject:OsmDenormalizedObject)                extends POIElement
final case class PlaceOfWorship(osmObject:OsmDenormalizedObject)     extends POIElement
final case class PoliceStation(osmObject:OsmDenormalizedObject)      extends POIElement
final case class PostBox(osmObject:OsmDenormalizedObject)            extends POIElement
final case class PostOffice(osmObject:OsmDenormalizedObject)         extends POIElement
final case class Prison(osmObject:OsmDenormalizedObject)             extends POIElement
final case class RecyclingContainer(osmObject:OsmDenormalizedObject) extends POIElement
final case class Sauna(osmObject:OsmDenormalizedObject)              extends POIElement
final case class Telephone(osmObject:OsmDenormalizedObject)          extends POIElement
final case class Toilet(osmObject:OsmDenormalizedObject)             extends POIElement
final case class GolfCourse(osmObject:OsmDenormalizedObject)         extends POIElement
final case class IceRink(osmObject:OsmDenormalizedObject)            extends POIElement
final case class Park(osmObject:OsmDenormalizedObject)               extends POIElement
final case class SportPitch(osmObject:OsmDenormalizedObject)         extends POIElement
final case class Playground(osmObject:OsmDenormalizedObject)         extends POIElement
final case class Stadium(osmObject:OsmDenormalizedObject)            extends POIElement
final case class SwimmingPool(osmObject:OsmDenormalizedObject)       extends POIElement
final case class SwimmingArea(osmObject:OsmDenormalizedObject)       extends POIElement
final case class SportTrack(osmObject:OsmDenormalizedObject)         extends POIElement
final case class Tree(osmObject:OsmDenormalizedObject)         extends POIElement


@typeclass trait POI[A] {
  def osmObj(p:A):OsmDenormalizedObject
  def name:String
  def fromOsmDenObj(oo:OsmDenormalizedObject):A
  def queryFromShape(shape:SourceGraph[A]):POIQuery[A]
  def bbsToQuery(rel:OsmDenormalizedRelation):List[(BoundingBox, Tag)]
}

case class Location(point:Point) extends Element

case class Tag(tag:OsmTag) extends Element

case class BoundingBox(hash:Long) extends Element

case class Id(id:OsmId) extends Element

case class Name(name:String) extends Element

