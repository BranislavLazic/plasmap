package io.plasmap.js

import java.nio.ByteBuffer

import upickle.default._
import io.plasmap.querymodel.{PMSerialiser, PMQuery}
import org.scalajs.dom
import org.scalajs.dom.MessageEvent

import scala.collection.immutable.Queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalaz.\/
import scalaz.\/.{fromTryCatchNonFatal => Try}

/**
 * Created by mark on 29.05.15.
 */
object PlasmapSocket {
  private[js] def onOpen(ev:dom.Event):Unit = {
  }
  private[js] def onError(ee:dom.ErrorEvent):Unit = {
    println(s"Encountered an error.")
  }
  private[js] def onClose(ce:dom.CloseEvent):Unit = {
  }
}

case class PlasmapSocket(
    url:String,
    onReceive:(Throwable \/ js.Object) => Unit,
    onComplete: (js.Array[js.Object])  => Unit) extends Socket {

  val buffer:js.Array[js.Object] = js.Array()

  var isOpen = false
  var queue  = Queue.empty[PMQuery]

  override private[js] def onMessage: (MessageEvent) => Unit = (me) => {
    println("Got message.")
    val msg = me.data.asInstanceOf[String]
    val response = Try[js.Object]( js.JSON.parse(msg).asInstanceOf[js.Object] )
    response.foreach(buffer.push(_))
    onReceive(response)
  }

  private[js] def onOpen      = (ev:dom.Event) => {
    println("opened connection")
    isOpen = true
    for( q <- queue ) send(q)
    queue = Queue.empty[PMQuery]
    ()
  }

  private[js] def onError     = PlasmapSocket.onError

  private[js] def onClose     = (_) => {
    println("closed connection")
    onComplete(buffer)
    isOpen = false
  }

  private[js] def send(q:PMQuery) = {
    println("Sending query")
    val serialised = PMSerialiser.serialiseQuery(q)
    ws.send(serialised)
  }

  @JSExport("send")
  def sendMessage(q:PMQuery)  =
    if (isOpen) send(q) else queue = queue enqueue q


}

