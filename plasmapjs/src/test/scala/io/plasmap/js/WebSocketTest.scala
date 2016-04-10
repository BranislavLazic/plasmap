package io.plasmap.js

import boopickle.Unpickle
import io.plasmap.querymodel.PMCityFromCoordinates
import utest._
import utest.framework.TestSuite

/**
 * Created by mark on 29.05.15.
 */
object WebSocketTest extends TestSuite {
    def tests = TestSuite {
      "Test Web Socket"-{
        println("---------------------------------------------")
        println("                                             ")
        val s = PlasmapSocket("ws://localhost:8000/api/websocket", x ⇒ println(s"Message received: $x"))
        s.sendMessage(PMCityFromCoordinates(1,3))
        println("                                             ")
        println("---------------------------------------------")
      }
    }
}
