package com.softwaremill.clippy

import java.io.File
import java.util.concurrent.TimeoutException

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.internal.util.Position
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class ClippyPlugin(val global: Global) extends Plugin {

  override val name: String = "clippy"

  override val description: String = "gives good advice"

  var url: String = ""
  var colorsConfig: ColorsConfig = ColorsConfig.Disabled
  var testMode = false
  val DefaultStoreDir = new File(System.getProperty("user.home"), ".clippy")
  var localStoreDir = DefaultStoreDir
  var projectRoot: Option[File] = None

  def handleError(pos: Position, msg: String): String = {
    val advices = loadAdvices(url, localStoreDir, projectRoot)
    val parsedMsg = CompilationErrorParser.parse(msg)
    val matchers = advices.map(_.errMatching.lift)
    val matches = matchers.flatMap(pf => parsedMsg.flatMap(pf))

    matches.size match {
      case 0 =>
        (parsedMsg, colorsConfig) match {
          case (Some(tme: TypeMismatchError[ExactT]), cc: ColorsConfig.Enabled) => prettyPrintTypeMismatchError(tme, msg, cc)
          case _ => msg
        }
      case 1 =>
        matches.mkString(s"$msg\n Clippy advises: ", "", "")
      case _ =>
        matches.mkString(s"$msg\n Clippy advises you to try one of these solutions: \n   ", "\n or\n   ", "")
    }
  }

  override def processOptions(options: List[String], error: (String) => Unit): Unit = {
    colorsConfig = colorsFromOptions(options)
    url = urlFromOptions(options)
    testMode = testModeFromOptions(options)
    localStoreDir = localStoreDirFromOptions(options)
    projectRoot = projectRootFromOptions(options)

    if (testMode) {
      val r = global.reporter
      global.reporter = new DelegatingReporter(r, handleError, colorsConfig)
    }
  }

  override val components: List[PluginComponent] = List(
    new InjectReporter(handleError, global) {
      override def colorsConfig = ClippyPlugin.this.colorsConfig
      override def isEnabled = !testMode
    }, new RestoreReporter(global) {
      override def isEnabled = !testMode
    }
  )

  private def prettyPrintTypeMismatchError(tme: TypeMismatchError[ExactT], msg: String, colors: ColorsConfig.Enabled): String = {
    val colorDiff = (s: String) => colors.diff(s).toString
    val plain = new StringDiff(tme.found.toString, tme.required.toString, colorDiff)

    val expandsMsg = if (tme.hasExpands) {
      val reqExpandsTo = tme.requiredExpandsTo.getOrElse(tme.required)
      val foundExpandsTo = tme.foundExpandsTo.getOrElse(tme.found)
      val expands = new StringDiff(foundExpandsTo.toString, reqExpandsTo.toString, colorDiff)
      s"""${expands.diff("\nExpanded types:\nfound   : %s\nrequired: %s\"")}"""
    }
    else
      ""

    s""" type mismatch;
         | Clippy advises, pay attention to the marked parts:
         | ${plain.diff("found   : %s\n required: %s")}$expandsMsg""".stripMargin
  }

  private def urlFromOptions(options: List[String]): String =
    options.find(_.startsWith("url=")).map(_.substring(4)).getOrElse("https://www.scala-clippy.org") + "/api/advices"

  private def colorsFromOptions(options: List[String]): ColorsConfig = {
    if (boolFromOptions(options, "colors")) {

      def colorToFansi(color: String): fansi.Attrs = color match {
        case "black" => fansi.Color.Black
        case "red" => fansi.Color.Red
        case "green" => fansi.Color.Green
        case "yellow" => fansi.Color.Yellow
        case "blue" => fansi.Color.Blue
        case "magenta" => fansi.Color.Magenta
        case "cyan" => fansi.Color.Cyan
        case "white" => fansi.Color.White
        case "light-gray" => fansi.Color.LightGray
        case "none" => fansi.Attrs.Empty
        case x =>
          global.warning("Unknown color: " + x)
          fansi.Attrs.Empty
      }

      val partColorPattern = "colors-(.*)=(.*)".r
      options.filter(_.startsWith("colors-")).foldLeft(ColorsConfig.defaultEnabled) {
        case (current, partAndColor) =>
          val partColorPattern(part, colorStr) = partAndColor
          val color = colorToFansi(colorStr.trim.toLowerCase())
          part.trim.toLowerCase match {
            case "diff" => current.copy(diff = color)
            case "comment" => current.copy(comment = color)
            case "type" => current.copy(`type` = color)
            case "literal" => current.copy(literal = color)
            case "keyword" => current.copy(keyword = color)
            case "reset" => current.copy(reset = color)
            case x =>
              global.warning("Unknown colored part: " + x)
              current
          }
      }
    }
    else ColorsConfig.Disabled
  }

  private def testModeFromOptions(options: List[String]): Boolean = boolFromOptions(options, "testmode")

  private def boolFromOptions(options: List[String], option: String): Boolean =
    options.find(_.startsWith(s"$option=")).map(_.substring(option.length + 1))
      .getOrElse("false")
      .toBoolean

  private def projectRootFromOptions(options: List[String]): Option[File] =
    options.find(_.startsWith("projectRoot=")).map(_.substring(12))
      .map(new File(_, ".clippy.json"))
      .filter(_.exists())

  private def localStoreDirFromOptions(options: List[String]): File =
    options.find(_.startsWith("store=")).map(_.substring(6)).map(new File(_)).getOrElse(DefaultStoreDir)

  private def loadAdvices(url: String, localStoreDir: File, projectAdviceFile: Option[File]): List[Advice] = {
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    try {
      Await
        .result(
          new AdviceLoader(global, url, localStoreDir, projectAdviceFile).load(),
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
