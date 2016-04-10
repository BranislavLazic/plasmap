package io.plasmap.js

import scala.scalajs.js.annotation.JSExport

/**
 * Created by mark on 25.02.15.
 */
object Helper {
  def makeVarArgsJSFun[A, B](f: scalajs.js.Function1[Seq[A], B]): scalajs.js.Function = {
    object holder {
      @JSExport
      def fun(args: A*): B = f(args)
    }
    val h = holder.asInstanceOf[scalajs.js.Dynamic]
    h.fun.bind(h).asInstanceOf[scalajs.js.Function]
  }
}
