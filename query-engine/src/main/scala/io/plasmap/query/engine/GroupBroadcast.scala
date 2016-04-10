package io.plasmap.query.engine

import akka.stream.FanOutShape.{Init, Name => FOSName}
import akka.stream.scaladsl.FlexiRoute
import akka.stream.{Outlet, Attributes, FanOutShape, Graph}

class GroupBroadcastShape[I](_init: Init[Boxed[I]] = FOSName[Boxed[I]]("Unzip"))
  extends FanOutShape[Boxed[I]](_init) {
  val outA: Outlet[Boxed[I]] = newOutlet[Boxed[I]]("outA")
  val outB: Outlet[Boxed[I]] = newOutlet[Boxed[I]]("outB")
  protected override def construct(i: Init[Boxed[I]]) = new GroupBroadcastShape(i)
}
class GroupBroadcast[I] extends FlexiRoute[Boxed[I], GroupBroadcastShape[I]](
  new GroupBroadcastShape, Attributes.name("Unzip")) {
  import FlexiRoute._

  override def createRouteLogic(p: PortT) = new RouteLogic[Boxed[I]] {

    val writeAllState: State[Any] = State[Any](DemandFromAll(p.outA, p.outB)) {
      (ctx, _, element) =>
        println(s"Got $element")
        ctx.emit(p.outA)(element)
        ctx.emit(p.outB)(element)
        SameState
    }

    override def initialState = writeAllState

    //override def initialCompletionHandling = eagerClose
  }

  override def withAttributes(attr: Attributes): Graph[GroupBroadcastShape[I], Unit] = throw new Error()
}