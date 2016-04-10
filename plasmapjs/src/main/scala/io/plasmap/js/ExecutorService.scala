package io.plasmap.js

import java.nio.ByteBuffer

import io.plasmap.querymodel.{PMSerialiser, PMQuery}
import org.scalajs.dom
import org.scalajs.dom.ext.{AjaxException, Ajax}
import org.scalajs.dom.raw.XMLHttpRequest
import upickle.default._


import scala.concurrent.{Promise, Future}

object ExecutorService {

  val host = "beta.plasmap.io"
  val port = 9000
  val timeout: Int = 60000

  def postWithAddress(address:String, query:PMQuery):Future[XMLHttpRequest] = {
    Ajax.post(
      url = s"http://$address",
      data = PMSerialiser.serialiseQuery(query),
      timeout = timeout,
      headers = Map[String, String](),
      withCredentials = false,
      responseType = ""
    )
  }
}
