package io.plasmap.js

import org.scalajs.dom

/**
 * Created by mark on 29.05.15.
 */

trait Socket {
  def url:String
  private[js] def onMessage: (dom.MessageEvent) ⇒ Unit
  private[js] def onOpen:    (dom.Event)        ⇒ Unit
  private[js] def onError:   (dom.ErrorEvent)   ⇒ Unit
  private[js] def onClose:   (dom.CloseEvent)   ⇒ Unit
  val ws = new dom.WebSocket(url)
//  ws.binaryType = "arraybuffer"
  ws.onmessage = onMessage
  ws.onopen = onOpen
  ws.onerror = onError
  ws.onclose = onClose
}
