package com.softwaremill.clippy

import java.io.File
import java.util.concurrent.TimeoutException

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.internal.util.Position
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.reporters.Reporter

class ClippyPlugin(val global: Global) extends Plugin {

  override val name: String = "clippy"

  override val components: List[PluginComponent] = Nil

  override val description: String = "gives good advice"

  override def init(options: List[String], error: (String) => Unit) = {
    val r = global.reporter

    val url = urlFromOptions(options)
    val localStoreDir = localStoreDirFromOptions(options)
    val advices = loadAdvices(url, localStoreDir)

    global.reporter = new Reporter {
      override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) = ???

      override def echo(msg: String) = r.echo(msg)
      override def comment(pos: Position, msg: String) = r.comment(pos, msg)
      override def hasErrors = r.hasErrors || cancelled
      override def reset() = {
        cancelled = false
        r.reset()
      }

      //

      override def echo(pos: Position, msg: String) = r.echo(pos, msg)
      override def warning(pos: Position, msg: String) = r.warning(pos, msg)
      override def errorCount = r.errorCount
      override def warningCount = r.warningCount
      override def hasWarnings = r.hasWarnings
      override def flush() = r.flush()
      override def count(severity: Severity): Int = r.count(conv(severity))
      override def resetCount(severity: Severity): Unit = r.resetCount(conv(severity))

      //

      private def conv(s: Severity): r.Severity = s match {
        case INFO => r.INFO
        case WARNING => r.WARNING
        case ERROR => r.ERROR
      }

      //

      override def error(pos: Position, msg: String) = r.error(pos, handleError(pos, msg))
    }

    def handleError(pos: Position, msg: String): String = {
      val totalMatchingFunction = advices.map(_.errMatching)
        .foldLeft(PartialFunction.empty[CompilationError[Exact], String])(_.orElse(_)).lift
      val adviceText = CompilationErrorParser.parse(msg).flatMap(totalMatchingFunction).map("\n Clippy advises: " + _).getOrElse("")
      msg + adviceText
    }

    true
  }

  private def urlFromOptions(options: List[String]): String =
    options.find(_.startsWith("url=")).map(_.substring(4)).getOrElse("https://www.scala-clippy.org") + "/api/advices"

  private def localStoreDirFromOptions(options: List[String]): File =
    options.find(_.startsWith("store=")).map(_.substring(6)).map(new File(_)).getOrElse {
      new File(System.getProperty("user.home"), ".clippy")
    }

  private def loadAdvices(url: String, localStoreDir: File): List[Advice] = {
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    try {
      Await
        .result(
          new AdviceLoader(global, url, localStoreDir).load(),
          10.seconds
        )
        .advices
    }
    catch {
      case e: TimeoutException =>
        global.warning(s"Unable to read advices from $url and store to $localStoreDir within 10 seconds.")
        Nil
      case e: Exception =>
        global.warning(s"Exception when reading advices from $url and storing to $localStoreDir: $e")
        Nil
    }
  }
}
