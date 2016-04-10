package io.plasmap.util.streams

import akka.stream.scaladsl.{Flow, Source}
import io.plasmap.util.streams.impl._


/**
 * Advanced utility functions for processing streams.
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
object StreamAdditions {

  implicit def toDeduplicator[I, O, M](flow: Flow[I, O, M]): FlowDeduplicator[I, O, M] = FlowDeduplicator(flow)

  implicit def toDeduplicator[O, M](source: Source[O, M]): SourceDeduplicator[O, M] = SourceDeduplicator(source)

  implicit def toDistinctCounter[I, O, M](flow: Flow[I, O, M]): FlowDistinctCounter[I, O, M] = FlowDistinctCounter(flow)

  implicit def toDistinctCounter[I, O, M](source: Source[O, M]): SourceDistinctCounter[I, O, M] = SourceDistinctCounter(source)

  implicit def toBranch[I, O, M](flow: Flow[I, O, M]): FlowBrancher[I, O, M] = FlowBrancher(flow)

}


