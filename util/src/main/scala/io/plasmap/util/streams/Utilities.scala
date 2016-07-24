package io.plasmap.util.streams

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}


object Utilities {


  def reduceByKey[In, K, Out](
                               maximumGroupSize: Int,
                               groupKey: (In) => K,
                               map: (In) => Out)(reduce: (Out, Out) => Out): Flow[In, (K, Out), NotUsed] = {
    Flow[In]
      .groupBy[K](maximumGroupSize, groupKey)
      .map(e => groupKey(e) -> map(e))
      .reduce((l, r) => l._1 -> reduce(l._2, r._2))
      .mergeSubstreams
  }

  def groupAndMapSubFlow[In, K, Out](groupKey: (In) => K,
                                     subFlow: Flow[In, Out, NotUsed],
                                     maximumGroupSize: Int
                                    ): Flow[In, Out, NotUsed] = {

    Flow[In]
      .groupBy[K](maximumGroupSize, groupKey)
      .via(subFlow)
      .mergeSubstreams
  }


  private[streams] def subflowWithGroupKey[In, K, Out](groupKey: (In) => K, subFlow: Flow[In, Out, NotUsed]):
  Flow[In, (K, Out), NotUsed] = Flow.fromGraph {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[In](outputPorts = 2, eagerCancel = true))
      val repeater = b.add(InfiniteRepeat[K])
      val zip = b.add(Zip[K, Out])

      broadcast ~> subFlow ~> zip.in1
      broadcast ~> Flow[In].map(groupKey) ~> repeater ~> zip.in0

      FlowShape(broadcast.in, zip.out)
    }
  }

  def groupAndMapSubflowWithKey[In, K, Out](
                                             groupKey: (In) => K,
                                             subFlow: Flow[In, Out, NotUsed],
                                             maximumGroupSize: Int): Flow[In, (K, Out), NotUsed] = {

    val subFlowWithGroupKey: Flow[In, (K, Out), NotUsed] = subflowWithGroupKey(groupKey, subFlow)

    groupAndMapSubFlow[In, K, (K, Out)](groupKey, subFlowWithGroupKey, maximumGroupSize)
  }


}
