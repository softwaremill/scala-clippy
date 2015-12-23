package org.softwaremill.clippy

import java.io.{PrintWriter, StringWriter, IOException}
import java.net.{URI, URL}
import scala.tools.nsc.Global
import scala.util.Try
import scala.xml._

sealed trait Advice {
  def errMatching: PartialFunction[CompilationError, String]
}

case class TypeMismatchAdvice(found: String, required: String, adviceText: String) extends Advice {

  override def errMatching = {
    case TypeMismatchError(errFound, errRequired) if errFound == found && errRequired == required => adviceText
  }
}

object Advices {

  def loadFromClasspath(global: Global): List[Advice] = {
    val allUrls = global.classPath.asURLs
    allUrls.filter(_.getPath.endsWith(".jar")).map(addClippyXml).toList.flatMap(loadFromURL(global))
  }

  private def addClippyXml(url: URL): URL = URI.create("jar:file://" + url.getPath + "!/clippy.xml").toURL

  def loadFromURL(global: Global)(xml: URL): List[Advice] = loadFromXml(parseXml(global, xml).getOrElse(Nil))

  def parseXml(global: Global, xml: URL): Try[NodeSeq] = {
    val loadResult = Try(XML.load(xml))
    if (loadResult.isFailure) {
      loadResult.failed.foreach {
        case e: IOException => ()
        case otherException =>
          val sw: StringWriter = new StringWriter()
          otherException.printStackTrace(new PrintWriter(sw))
          global.warning(s"Error when parsing $xml: $sw")
      }
    }
    loadResult
  }

  def loadFromXml(xml: NodeSeq): List[Advice] = {
    (xml \\ "typemismatch").map { n =>
      TypeMismatchAdvice(
        (n \ "found").text,
        (n \ "required").text,
        (n \ "advice").text
      )
    }.toList
  }

}