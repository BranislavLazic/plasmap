package io.plasmap.util.streams.impl

import akka.stream.scaladsl.Source
import com.yahoo.sketches.theta.UpdateSketch

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
case class SourceDistinctCounter[I,O,M](source:Source[O,M]) {

  /**
   * Counts the distinct elements emitted from the source using a probabilistic sketch. The count is memory bounded with size k * 8 bytes.
   * @param k The size of the hash, the higher the more accurate. See [[http://datasketches.github.io/docs/KMVupdateVkth.html]].
   * @return A source emitting count estimates
   */
  def countDistinct(k: Int = 4096, getId: (O) => Long): Source[Double, M] = {
    val sketch: UpdateSketch = UpdateSketch.builder.build(k)

    source
      .map((elem) => sketch.update(getId(elem)))
      .map(_ => sketch.getEstimate)
  }
}
