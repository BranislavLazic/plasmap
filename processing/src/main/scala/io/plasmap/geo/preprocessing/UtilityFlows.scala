package io.plasmap.geo.preprocessing

import akka.stream.scaladsl.Flow
import io.plasmap.model.{OsmRelation, OsmWay, OsmNode, OsmObject}
import io.plasmap.util.GeowUtils._

/**
 * Created by janschulte on 29/02/16.
 */
object UtilityFlows {

  val filterNode: Flow[OsmObject, OsmNode, Unit] = Flow[OsmObject]
    .filter(isNode)
    .map(_.asInstanceOf[OsmNode])
    .log(s"PassingFilter")

  val filterWay: Flow[OsmObject, OsmWay, Unit] = Flow[OsmObject]
    .filter(isWay)
    .map(_.asInstanceOf[OsmWay])
    .log(s"PassingFilter")

  val filterRelation: Flow[OsmObject, OsmRelation, Unit] = Flow[OsmObject]
    .filter(isRelation)
    .map(_.asInstanceOf[OsmRelation])
    .log(s"PassingFilter")
}
