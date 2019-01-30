package com.softwaremill.clippy

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.zip.GZIPInputStream

import com.softwaremill.clippy.Utils._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.tools.nsc.Global
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

class AdviceLoader(
    global: Global,
    url: String,
    localStoreDir: File,
    projectAdviceFile: Option[File],
    localAdviceFiles: List[URL]
)(implicit ec: ExecutionContext) {
  private val OneDayMillis = 1000L * 60 * 60 * 24

  private val localStore = new File(localStoreDir, "clippy.json.gz")

  private lazy val resourcesAdvice: AdvicesAndWarnings =
    localAdviceFiles.map(loadAdviceFromUrL).reduceOption(_ ++ _).getOrElse(AdvicesAndWarnings.empty)

  private def loadAdviceFromUrL(url: URL): AdvicesAndWarnings =
    TryWith(url.openStream())(inputStreamToClippy(_)) match {
      case Success(clippyData) => AdvicesAndWarnings(clippyData.advices, clippyData.fatalWarnings)
      case Failure(_) =>
        global.inform(s"Cannot load advice from ${url.getPath} : Ignoring.")
        AdvicesAndWarnings.empty
    }

  private lazy val projectAdvice: AdvicesAndWarnings =
    projectAdviceFile.map(file => loadAdviceFromUrL(file.toURI.toURL)).getOrElse(AdvicesAndWarnings.empty)

  def load(): Future[Clippy] = {
    val localClippy = if (!localStore.exists()) {
      fetchStoreParse()
    } else {
      val needsUpdate = System.currentTimeMillis() - localStore.lastModified() > OneDayMillis

      // fetching in the background
      val runningFetch = if (needsUpdate) {
        Some(fetchStoreParseInBackground())
      } else None

      val localLoad = Try(loadLocally()) match {
        case Success(v) => Future.successful(v)
        case Failure(t) => Future.failed(t)
      }

      localLoad.map(bytes => inputStreamToClippy(decodeZippedBytes(bytes))).recoverWith {
        case e: Exception =>
          global.warning(s"Cannot load advice from local store: $localStore. Trying to fetch from server")
          runningFetch.getOrElse(fetchStoreParse())
      }
    }

    // Add in advice found in resources and project root
    localClippy.map(
      clippy =>
        clippy.copy(
          advices = (projectAdvice.advices ++ resourcesAdvice.advices ++ clippy.advices).distinct,
          fatalWarnings =
            (projectAdvice.fatalWarnings ++ resourcesAdvice.fatalWarnings ++ clippy.fatalWarnings).distinct
      )
    )
  }

  private def fetchStoreParse(): Future[Clippy] =
    fetchCompressedJson()
      .map { bytes =>
        storeLocallyInBackground(bytes)
        bytes
      }
      .map(bytes => inputStreamToClippy(decodeZippedBytes(bytes)))
      .recover {
        case e: Exception =>
          global.inform(s"Unable to load/store local Clippy advice due to: ${e.getMessage}")
          Clippy(ClippyBuildInfo.version, Nil, Nil)
      }
      .andThen { case Success(v) => v.checkPluginVersion(ClippyBuildInfo.version, println) }

  private def fetchStoreParseInBackground(): Future[Clippy] = {
    val f = fetchStoreParse()
    f.onFailure {
      case e: Exception => global.inform(s"Cannot fetch data from $url due to: $e")
    }
    f
  }

  private def fetchCompressedJson(): Future[Array[Byte]] = Future {
    val u    = new URL(url)
    val conn = u.openConnection().asInstanceOf[HttpURLConnection]

    try {
      conn.setRequestMethod("GET")
      inputStreamToBytes(conn.getInputStream)
    } finally conn.disconnect()
  }

  private def decodeZippedBytes(bytes: Array[Byte]): GZIPInputStream = new GZIPInputStream(decodeUtf8Bytes(bytes))

  private def decodeUtf8Bytes(bytes: Array[Byte]): ByteArrayInputStream = new ByteArrayInputStream(bytes)

  private def inputStreamToClippy(byteStream: InputStream): Clippy = {
    import org.json4s.native.JsonMethods._
    val data = Source.fromInputStream(byteStream, "UTF-8").getLines().mkString("\n")
    Clippy
      .fromJson(parse(data))
      .getOrElse(throw new IllegalArgumentException("Cannot deserialize Clippy data"))
  }

  private def storeLocally(bytes: Array[Byte]): Unit = {
    if (!localStoreDir.isDirectory && !localStoreDir.mkdir()) {
      throw new IOException(s"Cannot create directory $localStoreDir")
    }
    TryWith(new FileOutputStream(localStore))(_.write(bytes)).get
  }

  private def storeLocallyInBackground(bytes: Array[Byte]): Unit =
    Future {
      runNonDaemon {
        storeLocally(bytes)
      }
    }.onFailure {
      case e: Exception => global.inform(s"Cannot store data at $localStore due to: $e")
    }

  private def loadLocally(source: File = localStore): Array[Byte] = inputStreamToBytes(new FileInputStream(source))
}

object AdviceLoader {
  val localFile = "clippy.json.gz"
}
