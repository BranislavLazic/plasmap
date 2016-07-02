package io.plasmap.geo.preprocessing.test

import io.plasmap.model.{OsmRelation, OsmWay}
import io.plasmap.parser.OsmParser


object TestFixtures {
  private val wayParser = OsmParser("processing/src/test/resources/ways-test.osm")
  lazy val ways: List[OsmWay] = (for {
    wayOpt <- wayParser
    way <- wayOpt
  } yield way).toList.collect { case way: OsmWay => way }.take(30)

  private val relationParser = OsmParser("processing/src/test/resources/relation-test.osm")
  lazy val relations: List[OsmRelation] = (for {
    relationOpt <- relationParser
    relation <- relationOpt
  } yield relation).toList.collect { case relation: OsmRelation => relation }.take(20)
}
