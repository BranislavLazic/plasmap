package io.plasmap.geo.util

import io.plasmap.model._

object KafkaTopics {

  val nodesTopic: String = "osm_nodes"
  val nodesFailedTopic: String = "osm_nodes_failed"
  val waysTopic: String = "osm_ways"
  val waysFailedTopic: String = "osm_ways_failed"
  val relationsTopic: String = "osm_relations"
  val relationsFailedTopic: String = "osm_relations_failed"

  val clientId = "preprocessing"
  val persisterTopic = "osm_denormalised"
  val persisterIndexGroup = "index"
  val persisterDataGroup = "data"
  val persisterDataByTagGroup = "data_tag"
  val persisterMappingGroup = "mapping"

  val consumerGroup: String = "preprocessing"

  def getTopic(typ: OsmType, failed: Boolean): String =
    if (failed) {
      getFailureTopic(typ)
    } else {
      getTopic(typ)
    }

  def getTopic(typ: OsmType): String = typ match {
    case OsmTypeNode => nodesTopic
    case OsmTypeWay => waysTopic
    case OsmTypeRelation => relationsTopic
  }

  def getFailureTopic(typ: OsmType): String = typ match {
    case OsmTypeNode => nodesFailedTopic
    case OsmTypeWay => waysFailedTopic
    case OsmTypeRelation => relationsFailedTopic
  }


}
