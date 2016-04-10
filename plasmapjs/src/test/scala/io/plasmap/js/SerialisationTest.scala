package io.plasmap.js

import io.plasmap.querymodel._
import utest._
import boopickle._

object SerialisationTest extends TestSuite{
  def tests = TestSuite {
    "can serialise and deserialise a simple city"-{
      val city = PMCityFromName("Frankfurt")
      val serialised = Pickle.intoBytes[PMQuery](city)
      println(serialised)
      val deserialised = Unpickle[PMQuery].fromBytes(serialised)
      println(s"\n\n$deserialised\n\n")
      assert(city == deserialised)
    }

    "can serialise and deserialise a composed query"-{
      val t1 = PMCityFromCoordinates(32.0f, 22.0f)
      val comp = PMDistrictsFromCity(t1)
      val serialised = Pickle.intoBytes[PMQuery](comp)
      val deserialised = Unpickle[PMQuery].fromBytes(serialised)
      assert(comp == deserialised)
    }
  }
}