package org.softwaremill.clippy

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
    allUrls.filter(_.getPath.endsWith(".jar")).map(addClippyXml).toList.flatMap(loadFromURL)
  }

  private def addClippyXml(url: URL): URL = URI.create("jar:file://" + url.getPath + "!/clippy.xml").toURL

  // TODO error handling for XML.load()
  def loadFromURL(xml: URL): List[Advice] = loadFromXml(Try(xml.openStream()).map(XML.load).getOrElse(Nil))

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