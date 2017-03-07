package org.softwaremill.clippy

import java.io.{FileOutputStream, File}
import java.util.zip.GZIPOutputStream
import scala.reflect.runtime.currentMirror
import com.softwaremill.clippy._
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import scala.tools.reflect.ToolBox
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
        TypeMismatchError(ExactT("slick.dbio.DBIOAction[*]"), None, ExactT("slick.lifted.Rep[Option[*]]"), None, None).asRegex,
        "Perhaps you forgot to call .result on your Rep[]? This will give you a DBIOAction that you can compose with other DBIOActions.",
        Library("com.typesafe.slick", "slick", "3.1.0")
      ),
      Advice(
        TypeMismatchError(ExactT("akka.http.scaladsl.server.StandardRoute"), None, ExactT("akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]"), None, None).asRegex,
        "did you forget to define an implicit akka.stream.ActorMaterializer? It allows routes to be converted into a flow. You can read more at http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0/scala/http/routing-dsl/index.html",
        Library("com.typesafe.akka", "akka-http-experimental", "2.0.0")
      ),
      Advice(
        NotFoundError(ExactT("value wire")).asRegex,
        "you need to import com.softwaremill.macwire._",
        Library("com.softwaremill.macwire", "macros", "2.0.0")
      ),
      Advice(
        NotFoundError(ExactT("value wire")).asRegex,
        "If you need further help check out the macwire readme at https://github.com/adamw/macwire",
        Library("com.softwaremill.macwire", "macros", "2.0.0")
      ),
      Advice(
        TypeArgumentsDoNotConformToOverloadedBoundsError(
        ExactT("*"), ExactT("value apply"), Set(
          ExactT("[E <: slick.lifted.AbstractTable[_]]=> slick.lifted.TableQuery[E]"),
          ExactT("[E <: slick.lifted.AbstractTable[_]](cons: slick.lifted.Tag => E)slick.lifted.TableQuery[E]")
        )
      ).asRegex,
        "incorrect class name passed to TableQuery",
        Library("com.typesafe.slick", "slick", "3.1.1")
      ),
      Advice(
        TypeclassNotFoundError(
        ExactT("Ordering"),
        ExactT("java.time.LocalDate")
      ).asRegex,
        "implicit val localDateOrdering: Ordering[java.time.LocalDate] = Ordering.by(_.toEpochDay)",
        Library("java-lang", "time", "8+")
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
    "akka http" ->
      """
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
    "macwire" ->
      """
        |class A()
        |val a = wire[A]
      """.stripMargin,
    "slick" ->
      """
        |case class User(id1: Long, id2: Long)
        |trait TestSchema {
        |
        |  val db: slick.jdbc.JdbcBackend#DatabaseDef
        |  val driver: slick.driver.JdbcProfile
        |
        |  import driver.api._
        |
        |  protected val users = TableQuery[User]
        |
        |  protected class Users(tag: Tag) extends Table[User](tag, "users") {
        |    def id1 = column[Long]("id")
        |    def id2 = column[Long]("id")
        |
        |    def * = (id1, id2) <> (User.tupled, User.unapply)
        |  }
        |}
      """.stripMargin,
    "Type mismatch pretty diff" ->
      """
        |class Test {
        |
        |  type Cool = (String, String, Int, Option[String], Long)
        |  type Bool = (String, String, Int, String, Long)
        |
        |  def test(cool: Cool): Bool = cool
        |
        |}
      """.stripMargin
  )

  val tb = {
    val cpp = sys.env("CLIPPY_PLUGIN_PATH")
    currentMirror.mkToolBox(options = s"-Xplugin:$cpp -Xplugin-require:clippy -P:clippy:colors=true -P:clippy:testmode=true")
  }

  def tryCompile(snippet: String) = tb.compile(tb.parse(snippet))

  for ((name, s) <- snippets) {
    name should "compile with errors" in {
      (the[ToolBoxError] thrownBy tryCompile(s)).message should include("Clippy advises")
    }
  }

  "Clippy" should "return all matching advice" in {
    (the[ToolBoxError] thrownBy tryCompile(snippets("macwire")))
      .message should include("Clippy advises you to try one of these")
  }

}
