package io.plasmap.util.streams.impl

import akka.stream.scaladsl.Flow
import breeze.util.BloomFilter

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
case class FlowDeduplicator[I,O,M](flow:Flow[I,O,M]) {

  /**
   * Deduplicates the flow using a probabilistic bloom filter
   * @param expectedNumItems the number of distinct items expected that pass through the flow
   * @param falsePositiveRate the desired rate of false positives
   * @return a flow with removed duplicates
   */
  def deduplicate(expectedNumItems: Double = 1000, falsePositiveRate: Double = 0.001): Flow[I, O, M] = {
    val bloom: BloomFilter[O] = BloomFilter.optimallySized[O](expectedNumItems,falsePositiveRate)

    flow
      .filter((elem) => {
        if (!bloom.contains(elem)) {
          bloom += elem
          true
        } else {
          false
        }
      }
      )
  }
}
