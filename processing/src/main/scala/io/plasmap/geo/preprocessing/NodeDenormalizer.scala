package io.plasmap.geo.preprocessing

import akka.stream.scaladsl.Flow
import io.plasmap.geo.preprocessing.UtilityFlows._
import io.plasmap.model._
import io.plasmap.util.Denormalizer

import scalaz.\/
import scalaz.syntax.either._

/**
  * Created by mark on 30.10.15.
  */
case class NodeDenormalizer() {

  val flow: Flow[OsmObject, FlowError \/ OsmDenormalizedNode, Unit] =
    Flow[OsmObject]
      .via(filterNode)
      .map((node) => Denormalizer.denormalizeNode(node).right[FlowError])
}
