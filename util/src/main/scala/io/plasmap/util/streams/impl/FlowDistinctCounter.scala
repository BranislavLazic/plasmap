package io.plasmap.util.streams.impl

import akka.stream.scaladsl.Flow
import com.yahoo.sketches.theta.UpdateSketch

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
case class FlowDistinctCounter[I,O,M](flow:Flow[I,O,M]) {

  /**
   * Counts the distinct elements of a flow using a probabilistic sketch. The count is memory bounded with size k * 8 bytes.
   * @param k The size of the hash, the higher the more accurate. See [[http://datasketches.github.io/docs/KMVupdateVkth.html]].
   * @return A Flow emitting count estimates
   */
  def countDistinct(k: Int = 4096, getId: (O) => Long): Flow[I, Double, M] = {
    val sketch: UpdateSketch = UpdateSketch.builder.build(k)

    flow
    .map((elem) => sketch.update(getId(elem)))
    .map(_ => sketch.getEstimate)
  }
}
