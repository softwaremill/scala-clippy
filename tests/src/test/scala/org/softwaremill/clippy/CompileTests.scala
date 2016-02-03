package org.softwaremill.clippy

import java.io.{FileOutputStream, File}
import java.util.zip.GZIPOutputStream

import com.softwaremill.clippy._
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}

import scala.tools.reflect.ToolBoxError

class CompileTests extends FlatSpec with Matchers with BeforeAndAfterAll {

  val localStoreDir = new File(System.getProperty("user.home"), ".clippy")
  val localStore = new File(localStoreDir, "clippy.json.gz")
  val localStore2 = new File(localStoreDir, "clippy2.json.gz")

  /**
   * Writing test json data to where the plugin will expect to have it cached.
   */
  override protected def beforeAll() = {
    super.beforeAll()
    localStoreDir.mkdirs()
    if (localStore.exists()) {
      localStore.renameTo(localStore2)
    }

    val advices = List(
      Advice(
        1L,
        TypeMismatchError(ExactT("slick.dbio.DBIOAction[*]"), None, ExactT("slick.lifted.Rep[Option[*]]"), None).asRegex,
        "Perhaps you forgot to call .result on your Rep[]? This will give you a DBIOAction that you can compose with other DBIOActions.",
        Library("com.typesafe.slick", "slick", "3.1.0")
      ),
      Advice(
        2L,
        TypeMismatchError(ExactT("akka.http.scaladsl.server.StandardRoute"), None, ExactT("akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]"), None).asRegex,
        "did you forget to define an implicit akka.stream.ActorMaterializer? It allows routes to be converted into a flow. You can read more at http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0/scala/http/routing-dsl/index.html",
        Library("com.typesafe.akka", "akka-http-experimental", "2.0.0")
      ),
      Advice(
        3L,
        NotFoundError(ExactT("value wire")).asRegex,
        "you need to import com.softwaremill.macwire._",
        Library("com.softwaremill.macwire", "macros", "2.0.0")
      )
    )

    import org.json4s.native.JsonMethods._
    val data = compact(render(Clippy("0.1", advices).toJson))

    val os = new GZIPOutputStream(new FileOutputStream(localStore))
    try os.write(data.getBytes("UTF-8")) finally os.close()
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
