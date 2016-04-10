package io.plasmap.util.streams.impl

import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, FlowGraph}

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
case class FlowBrancher[I, O, M](flow: Flow[I, O, M]) {

  import FlowGraph.Implicits._

  /**
   * Branch the flow based on the predicate function.
   * The left flow contains all elements not passing the filter.
   * The right flow contains all elements passing the flow.
   * @param predicate
   * @return
   */
  def branch(predicate: (O) => Boolean): (Flow[I, O, Unit], Flow[I, O, Unit]) = {
    val rightFlow: Flow[I, O, Unit] = Flow() { implicit builder =>

      val source: FlowShape[I, O] = builder.add(flow)
      val sink = builder.add(Flow[O])

      source ~> Flow[O].filter((x) =>  predicate(x)) ~> sink

      (source.inlet,sink.outlet)
    }

    val leftFlow: Flow[I, O, Unit] = Flow() { implicit builder =>

      val source: FlowShape[I, O] = builder.add(flow)
      val sink = builder.add(Flow[O])

      source ~> Flow[O].filter((x) =>  !predicate(x)) ~> sink

      (source.inlet,sink.outlet)
    }

    (leftFlow,rightFlow)
  }

}
