package com.softwaremill.clippy

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.zip.GZIPInputStream

import com.softwaremill.clippy.Utils._

import scala.concurrent.{ExecutionContext, Future}
import scala.tools.nsc.Global
import scala.util.{Success, Try}
import scala.xml.XML

class AdviceLoader(global: Global, url: String, localStoreDir: File)(implicit ec: ExecutionContext) {
  private val OneDayMillis = 1000L * 60 * 60 * 24

  private val localStore = new File(localStoreDir, "clippy.xml.gz")

  def load(): Future[Clippy] = {
    val result = if (!localStore.exists()) {
      fetchStoreParse()
    }
    else {
      val needsUpdate = System.currentTimeMillis() - localStore.lastModified() > OneDayMillis

      // fetching in the background
      val runningFetch = if (needsUpdate) {
        Some(fetchStoreParseInBackground())
      }
      else None

      Future.fromTry(Try(loadLocally())).map(bytesToClippy).recoverWith {
        case e: Exception =>
          global.warning(s"Cannot load advice from local store: $localStore. Trying to fetch from server")
          runningFetch.getOrElse(fetchStoreParse())
      }
    }

    result.andThen { case Success(v) => v.checkPluginVersion(global.warning) }
  }

  private def fetchStoreParse(): Future[Clippy] =
    fetchCompressedXml()
      .map { bytes =>
        storeLocallyInBackground(bytes)
        bytes
      }
      .map(bytesToClippy)

  private def fetchStoreParseInBackground(): Future[Clippy] = {
    val f = fetchStoreParse()
    f.onFailure {
      case e: Exception => global.warning(s"Cannot fetch data from $url due to: $e")
    }
    f
  }

  private def fetchCompressedXml(): Future[Array[Byte]] = Future {
    val u = new URL(url)
    val conn = u.openConnection().asInstanceOf[HttpURLConnection]

    try {
      conn.setRequestMethod("GET")
      inputStreamToBytes(conn.getInputStream)
    }
    finally conn.disconnect()
  }

  private def bytesToClippy(bytes: Array[Byte]): Clippy = {
    val nodeSeq = XML.load(new GZIPInputStream(new ByteArrayInputStream(bytes)))
    Clippy.fromXml(nodeSeq)
  }

  private def storeLocally(bytes: Array[Byte]): Unit = {
    if (!localStoreDir.isDirectory && !localStoreDir.mkdir()) {
      throw new IOException(s"Cannot create directory $localStoreDir")
    }
    val os = new FileOutputStream(localStore)
    try os.write(bytes) finally os.close()
  }

  private def storeLocallyInBackground(bytes: Array[Byte]): Unit = {
    Future {
      runNonDaemon {
        storeLocally(bytes)
      }
    }.onFailure {
      case e: Exception => global.warning(s"Cannot store data at $localStore due to: $e")
    }
  }

  private def loadLocally(): Array[Byte] = inputStreamToBytes(new FileInputStream(localStore))
}
