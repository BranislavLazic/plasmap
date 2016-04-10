package io.plasmap.js

/**
 * Created by mark on 22.05.15.
 */

import java.nio.ByteBuffer

import io.plasmap.querymodel.{PMSerialiser, PMQuery}
import org.scalajs.dom
import org.scalajs.dom.ext.AjaxException

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}


object Serialiser {
  def serialiseQuery(q:PMQuery): String = PMSerialiser.serialiseQuery(q)
  def deserialiseQuery(q:String): PMQuery = PMSerialiser.deserialiseQuery(q).toOption.get
}

