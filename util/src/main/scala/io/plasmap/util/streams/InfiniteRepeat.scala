package io.plasmap.util.streams

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}


final class InfiniteRepeat[In](extrapolate: In ⇒ Iterator[In]) extends GraphStage[FlowShape[In, In]] {

  private val in = Inlet[In]("expand.in")
  private val out = Outlet[In]("expand.out")

  override def initialAttributes = Attributes.none
  override val shape = FlowShape(in, out)

  override def createLogic(attr: Attributes) = new GraphStageLogic(shape) {
    private var iterator: Iterator[In] = Iterator.empty
    private var expanded = false

    override def preStart(): Unit = pull(in)

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        iterator = extrapolate(grab(in))
        if (iterator.hasNext) {
          if (isAvailable(out)) {
            expanded = true
            pull(in)
            push(out, iterator.next())
          } else expanded = false
        } else pull(in)
      }
      override def onUpstreamFinish(): Unit = {

      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (iterator.hasNext) {
          if (!expanded) {
            expanded = true
            if (isClosed(in)) {
              push(out, iterator.next())
              completeStage()
            } else {
              // expand needs to pull first to be “fair” when upstream is not actually slow
              pull(in)
              push(out, iterator.next())
            }
          } else push(out, iterator.next())
        }
      }
    })
  }
}

object InfiniteRepeat {

  def apply[In]:InfiniteRepeat[In] = new InfiniteRepeat(Iterator.continually[In](_))

}