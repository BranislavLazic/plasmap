package io.plasmap.query.engine

import akka.stream.{Inlet, Attributes, FanInShape}
import akka.stream.FanInShape.{Init, Name => FISName}
import akka.stream.scaladsl.FlexiMerge

class GroupMergeShape[A,B](_init: Init[Boxed[(A, B)]] = FISName("Zip"))
  extends FanInShape[Boxed[(A, B)]](_init) {
  val left = newInlet[Boxed[A]]("left")
  val right = newInlet[Boxed[B]]("right")
  protected override def construct(i: Init[Boxed[(A, B)]]) = new GroupMergeShape(i)
}

class GroupMerge[A, B] extends FlexiMerge[Boxed[(A, B)], GroupMergeShape[A, B]](
  new GroupMergeShape, Attributes.name("Zip2State")) {
  import FlexiMerge._


  override def createMergeLogic(p: PortT) = new MergeLogic[Boxed[(A, B)]] {
    var lastInA: A = _

    var elements = Map.empty[(Int,Long),Boxed[A]]

    private val left: Inlet[Boxed[A]] = p.left
    private val right: Inlet[Boxed[B]] = p.right

    println(s"Port: $left\nPort hash: ${left.hashCode()}")
    println(s"Port: $right\nPort hash: ${right.hashCode()}")

    val readAll = State(ReadAny(left, right)) { (ctx, port, inputs) =>


      val leftInlet = p.left
      val rightInlet = p.right

      if (port == leftInlet) {
        val a: Boxed[A] = inputs
        val tuple: ((Int, Long), Boxed[A]) = a.current -> a
        elements = elements + tuple
        println(s"Not emmitting. Current elements: $elements")
        SameState
      } else {
        val groupValueElement: Boxed[B] = inputs
        val groupKey = groupValueElement.previous.getOrElse(0, 0L)
        val groupKeyElementOpt: Option[Boxed[A]] = elements.get(0->groupKey)

        groupKeyElementOpt match {
          case Some(groupKeyElement) =>
            val groupedElement: (A, B) = groupKeyElement.element -> groupValueElement.element
            val x: Map[Int, Long] = groupKeyElement.previous ++ groupValueElement.previous
            val boxed = Boxed(groupedElement,2->2,x)
            println(s"Emitting $boxed")
            ctx.emit(boxed)

          case None =>
            println(s"No group key found")
        }

        SameState
      }
    }

    override def initialState: State[_] = readAll

    //override def initialCompletionHandling = eagerClose
  }
}