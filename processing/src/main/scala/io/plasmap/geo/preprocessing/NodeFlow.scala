package io.plasmap.geo.preprocessing

import akka.NotUsed
import akka.stream.scaladsl.Flow
import io.plasmap.geo.preprocessing.UtilityFlows._
import io.plasmap.model._
import io.plasmap.util.Denormalizer

import scalaz.\/
import scalaz.syntax.either._

object NodeFlow {

  val denormaliseNodeFlow: Flow[OsmNode, FlowError \/ OsmDenormalizedNode, NotUsed] =
    Flow[OsmNode]
      .map((node) => Denormalizer.denormalizeNode(node).right[FlowError])
}
