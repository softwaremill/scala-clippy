package org.softwaremill.clippy

import java.io.{FileOutputStream, File}
import java.util.zip.GZIPOutputStream

import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}

import scala.io.Source
import scala.tools.reflect.ToolBoxError

class CompileTests extends FlatSpec with Matchers with BeforeAndAfterAll {

  val localStoreDir = new File(System.getProperty("user.home"), ".clippy")
  val localStore = new File(localStoreDir, "clippy.xml.gz")
  val localStore2 = new File(localStoreDir, "clippy2.xml.gz")

  /**
   * Writing test xml data to where the plugin will expect to have it cached.
   */
  override protected def beforeAll() = {
    super.beforeAll()
    localStoreDir.mkdirs()
    if (localStore.exists()) {
      localStore.renameTo(localStore2)
    }

    val clippyXml = Source.fromInputStream(this.getClass.getResourceAsStream("/clippy.xml")).getLines().mkString("\n")
    val os = new GZIPOutputStream(new FileOutputStream(localStore))
    try os.write(clippyXml.getBytes("UTF-8")) finally os.close()
  }

  override protected def afterAll() = {
    localStore.delete()
    if (localStore2.exists()) {
      localStore2.renameTo(localStore)
    }

    super.afterAll()
  }

  val snippets = Map(
    "akka http" -> """
                     |import akka.actor.ActorSystem
                     |import akka.http.scaladsl.Http
                     |
                     |import akka.http.scaladsl.server.Directives._
                     |
                     |implicit val system = ActorSystem()
                     |
                     |val r = complete("ok")
                     |
                     |Http().bindAndHandle(r, "localhost", 8080)
                   """.stripMargin,
    "macwire" -> """
                     |class A()
                     |val a = wire[A]
                   """.stripMargin
  )

  for ((name, s) <- snippets) {
    name should "compile with errors" in {
      val cpp = sys.env("CLIPPY_PLUGIN_PATH")

      import scala.reflect.runtime._
      val cm = universe.runtimeMirror(getClass.getClassLoader)

      import scala.tools.reflect.ToolBox
      val tb = cm.mkToolBox(options = s"-Xplugin:$cpp -Xplugin-require:clippy")

      try {
        tb.eval(tb.parse(s))
        fail("Should report compile errors")
      }
      catch {
        case e: ToolBoxError =>
          e.message should include("Clippy advises")
      }
    }
  }
}
