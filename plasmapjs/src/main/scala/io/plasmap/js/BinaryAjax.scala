package io.plasmap.js

import java.nio.ByteBuffer

import io.plasmap.querymodel.PMQuery
import org.scalajs.dom
import org.scalajs.dom.ext.AjaxException
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.concurrent.Promise

/**
 * Created by mark on 26.05.15.
 */
object BinaryAjax {
  //Binary data support for Scala.js DOM 0.6.2
  def post(url:String,
           data:ByteBuffer,
           timeout:Int = 0,
           headers:Map[String, String] = Map.empty,
           withCredentials: Boolean = false,
           responseType: String = "" ) = {
    val req = new dom.XMLHttpRequest()
    val promise = Promise[dom.XMLHttpRequest]()

    req.onreadystatechange = { (e:dom.Event) ⇒
      if (req.readyState == 4) {
        if ((req.status >= 200 && req.status < 300) || req.status == 304)
          promise success req
        else
          promise failure AjaxException(req)
      }
    }
    req.open("POST", url)
    req.responseType = responseType
    req.timeout = timeout
    req.withCredentials = withCredentials
    for((header, value) ← headers) req.setRequestHeader(header, value)
    req.send(data.typedArray())
    promise.future
  }
}
