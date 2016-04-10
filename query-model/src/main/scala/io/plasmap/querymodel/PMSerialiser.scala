package io.plasmap.querymodel

import java.nio.ByteBuffer

import upickle.default._

import scalaz.\/

/**
 * Created by janschulte on 16/07/15.
 */
object PMSerialiser {

  def deserialiseQuery(msg: String): Throwable \/ PMQuery = {
    \/.fromTryCatchNonFatal(read[PMQuery](msg))
  }

  def serialiseQuery(query:PMQuery):String = {
    write[PMQuery](query)
  }

}
