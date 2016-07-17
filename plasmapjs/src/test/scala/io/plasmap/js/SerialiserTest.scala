package io.plasmap.js

import io.plasmap.querymodel.PMCityFromName
import utest._

/**
 * Created by mark on 22.05.15.
 */

object SerialiserTest extends TestSuite {
  override def tests = TestSuite {
    "The Serialiser should work with queries"-{
      def roundtrip = Serialiser.serialiseQuery _ andThen Serialiser.deserialiseQuery
      val query = PMCityFromName("Merkel")
      assert(roundtrip(query) == query)
    }
  }
}
