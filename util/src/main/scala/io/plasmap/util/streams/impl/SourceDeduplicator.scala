package io.plasmap.util.streams.impl

import akka.stream.scaladsl.Source
import breeze.util.BloomFilter

/**
 * @author Jan Schulte <jan@plasmap.io>
 */
case class SourceDeduplicator[O,M](source:Source[O,M]) {

  /**
   * Deduplicates the source using a probabilistic bloom filter
   * @param expectedNumItems the number of distinct items expected from the source
   * @param falsePositiveRate the desired rate of false positives
   * @return a source with removed duplicates
   */
  def deduplicate(expectedNumItems: Double = 1000, falsePositiveRate: Double = 0.001): Source[O, M] = {
    val bloom: BloomFilter[O] = BloomFilter.optimallySized[O](expectedNumItems,falsePositiveRate)

    source
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
