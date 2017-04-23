package com.softwaremill.clippy

import java.io._
import java.io.File
import java.net.{HttpURLConnection, URL}
import java.util.zip.GZIPInputStream

import com.softwaremill.clippy.Utils._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.tools.nsc.Global
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

class AdviceLoader(global: Global, url: String, localStoreDir: File, projectAdviceFile: Option[File])(implicit ec: ExecutionContext) {
  private val OneDayMillis = 1000L * 60 * 60 * 24

  private val localStore = new File(localStoreDir, "clippy.json.gz")

  private def listFilesForFolder(folder: File): List[File] = {
    folder.listFiles.flatMap { fileEntry =>
      if (fileEntry.isDirectory)
        listFilesForFolder(fileEntry)
      else List(fileEntry)
    }.toList
  }

  private val resourcesAdvice: List[Advice] =
    getClass.getClassLoader
      .getResources("")
      .asScala
      .toList
      .flatMap { entry =>
        val file = new File(entry.getFile)
        listFilesForFolder(file)
      }
      .filter(_.toString.endsWith("clippy.json"))
      .flatMap(file => loadAdviceFromUrl(file.toURI.toURL))

  private def loadAdviceFromUrl(url: URL): List[Advice] =
    TryWith(url.openStream())(inputStreamToClippy(_).advices) match {
      case Success(advices) => advices
      case Failure(_) =>
        global.inform(s"Cannot load advice from ${url.getPath} : Ignoring.")
        Nil
    }

  private val projectAdvice: List[Advice] =
    projectAdviceFile.map(file =>
      loadAdviceFromUrl(file.toURI.toURL)).getOrElse(Nil)

  def load(): Future[Clippy] = {
    val localClippy = if (!localStore.exists()) {
      fetchStoreParse()
    }
    else {
      val needsUpdate = System.currentTimeMillis() - localStore.lastModified() > OneDayMillis

      // fetching in the background
      val runningFetch = if (needsUpdate) {
        Some(fetchStoreParseInBackground())
      }
      else None

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
    localClippy.map(clippy =>
      clippy.copy(advices = (projectAdvice ++ resourcesAdvice ++ clippy.advices).distinct))
  }

  private def fetchStoreParse(): Future[Clippy] =
    fetchCompressedJson()
      .map { bytes =>
        storeLocallyInBackground(bytes)
        bytes
      }
      .map(bytes => inputStreamToClippy(decodeZippedBytes(bytes)))
      .recover{
        case e: Exception =>
          global.inform(s"Unable to load/store local Clippy advice due to: ${e.getMessage}")
          Clippy(ClippyBuildInfo.version, Nil)
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
    val u = new URL(url)
    val conn = u.openConnection().asInstanceOf[HttpURLConnection]

    try {
      conn.setRequestMethod("GET")
      inputStreamToBytes(conn.getInputStream)
    }
    finally conn.disconnect()
  }

  private def decodeZippedBytes(bytes: Array[Byte]): GZIPInputStream = new GZIPInputStream(decodeUtf8Bytes(bytes))

  private def decodeUtf8Bytes(bytes: Array[Byte]): ByteArrayInputStream = new ByteArrayInputStream(bytes)

  private def inputStreamToClippy(byteStream: InputStream): Clippy = {
    import org.json4s.native.JsonMethods._
    val data = Source.fromInputStream(byteStream, "UTF-8").getLines().mkString("\n")
    Clippy.fromJson(parse(data))
      .getOrElse(throw new IllegalArgumentException("Cannot deserialize Clippy data"))
  }

  private def storeLocally(bytes: Array[Byte]): Unit = {
    if (!localStoreDir.isDirectory && !localStoreDir.mkdir()) {
      throw new IOException(s"Cannot create directory $localStoreDir")
    }
    TryWith(new FileOutputStream(localStore))(_.write(bytes)).get
  }

  private def storeLocallyInBackground(bytes: Array[Byte]): Unit = {
    Future {
      runNonDaemon {
        storeLocally(bytes)
      }
    }.onFailure {
      case e: Exception => global.inform(s"Cannot store data at $localStore due to: $e")
    }
  }

  private def loadLocally(source: File = localStore): Array[Byte] = inputStreamToBytes(new FileInputStream(source))
}
