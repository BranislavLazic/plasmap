package io.plasmap.geo.producing.test

import _root_.io.plasmap.generator.OsmObjectGenerator
import _root_.io.plasmap.geo.producing.OsmProducer
import _root_.io.plasmap.model.OsmObject
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

/**
 * Specification for Queries
 */
class OsmProducerSpec
  extends Specification
  with IsolatedMockFactory
  with ProxyMockFactory {

  sequential

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  val gen = OsmObjectGenerator()
/*

  "The OsmProducer" should {

    "branch osm objects to the correct sink based on their type" in {

      val node = gen.generateNode
      val way = gen.generateWay
      val relation = gen.generateRelation

      val objSource = Source(List(node,way,relation))

      val nodesBuf = scala.collection.mutable.ArrayBuffer.empty[OsmObject]
      val waysBuf = scala.collection.mutable.ArrayBuffer.empty[OsmObject]
      val relationsBuf = scala.collection.mutable.ArrayBuffer.empty[OsmObject]

      val nodesSink = Sink.foreach[OsmObject](nodesBuf.append(_))
      val waysSink = Sink.foreach[OsmObject](waysBuf.append(_))
      val relationsSink = Sink.foreach[OsmObject](relationsBuf.append(_))

      val flowUnderTest = OsmProducer.osmJunction(objSource,nodesSink,waysSink,relationsSink)

      flowUnderTest.run()

      nodesBuf.map(_.toString) must contain(node.toString)
      nodesBuf must not contain(way)
      nodesBuf must not contain(relation)
      waysBuf must contain(way)
      waysBuf must not contain(node)
      waysBuf must not contain(relation)
      relationsBuf must contain(relation)
      relationsBuf must not contain(node)
      relationsBuf must not contain(way)
    }

  }
*/


}
