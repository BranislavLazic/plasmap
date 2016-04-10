package io.plasmap.util

import java.nio.ByteBuffer

/**
 * Utilities for serialisation
 * @author Jan Schulte <jan@plasmap.io>
 */
object SerialisationUtils {

  def toBytes(buffer: ByteBuffer): Array[Byte] = {
    // Don't do buffer.array() here. This returns null (yes, indeed wtf!)
    val buf = buffer.duplicate()
    val ret = Array.ofDim[Byte](buf.remaining())
    buf.get(ret)
    ret
  }
}
