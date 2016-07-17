package io.plasmap.util

import io.plasmap.model._

sealed class NormalisedOrDenormalised[T]

object NormalisedOrDenormalised {

  implicit object NormalisedWitness extends NormalisedOrDenormalised[OsmObject]
  implicit object NormalisedNodeWitness extends NormalisedOrDenormalised[OsmNode]
  implicit object NormalisedWayWitness extends NormalisedOrDenormalised[OsmWay]
  implicit object NormalisedRelationWitness extends NormalisedOrDenormalised[OsmRelation]

  implicit object DenormalisedWitness extends NormalisedOrDenormalised[OsmDenormalizedObject]
  implicit object DenormalisedNodeWitness extends NormalisedOrDenormalised[OsmDenormalizedNode]
  implicit object DenormalisedWayWitness extends NormalisedOrDenormalised[OsmDenormalizedWay]
  implicit object DenormalisedRelationWitness extends NormalisedOrDenormalised[OsmDenormalizedRelation]

}