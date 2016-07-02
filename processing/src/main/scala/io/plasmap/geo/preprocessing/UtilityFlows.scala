package io.plasmap.geo.preprocessing

import akka.NotUsed
import akka.stream.scaladsl.Flow
import io.plasmap.model.{OsmNode, OsmObject, OsmRelation, OsmWay}
import io.plasmap.util.GeowUtils._

/**
 * Created by janschulte on 29/02/16.
 */
object UtilityFlows {

  val filterNode: Flow[OsmObject, OsmNode, NotUsed] = Flow[OsmObject]
    .filter(isNode)
    .map(_.asInstanceOf[OsmNode])
    .log(s"PassingFilter")

  val filterWay: Flow[OsmObject, OsmWay, NotUsed] = Flow[OsmObject]
    .filter(isWay)
    .map(_.asInstanceOf[OsmWay])
    .log(s"PassingFilter")

  val filterRelation: Flow[OsmObject, OsmRelation, NotUsed] = Flow[OsmObject]
    .filter(isRelation)
    .map(_.asInstanceOf[OsmRelation])
    .log(s"PassingFilter")
}
