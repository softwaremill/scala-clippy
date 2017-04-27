package util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object Zip {
  private val BufferSize = 512

  def compress(string: String): Array[Byte] = {
    val os  = new ByteArrayOutputStream(string.length() / 5)
    val gos = new GZIPOutputStream(os)
    gos.write(string.getBytes("UTF-8"))
    gos.close()
    os.close()
    os.toByteArray
  }

  def decompress(compressed: Array[Byte]): String = {
    val is        = new ByteArrayInputStream(compressed)
    val gis       = new GZIPInputStream(is, BufferSize)
    val string    = new StringBuilder()
    val data      = new Array[Byte](BufferSize)
    var bytesRead = gis.read(data)
    while (bytesRead != -1) {
      string.append(new String(data, 0, bytesRead, "UTF-8"))
      bytesRead = gis.read(data)
    }
    gis.close()
    is.close()
    string.toString()
  }
}
