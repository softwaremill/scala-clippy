package org.softwaremill.clippy

import org.scalatest.{Matchers, FlatSpec}

import scala.tools.reflect.ToolBoxError

class CompileTests extends FlatSpec with Matchers {

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
                   """.stripMargin
  //    "macwire" -> """
  //                   |class A()
  //                   |val a = wire[A]
  //                 """.stripMargin // TODO: old clippy.xml format
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
