package org.softwaremill.clippy

import java.net.URL

import scala.xml._
import scala.collection.JavaConversions._

sealed trait Advice
case class TypeMismatchAdvice(found: String, required: String, advice: String) extends Advice

object Advices {
  def loadFromClasspath(): List[Advice] = {
    val i = enumerationAsScalaIterator(this.getClass.getClassLoader.getResources("clippy.xml"))
    i.toList.flatMap(loadFromURL)
  }

  def loadFromURL(url: URL): List[Advice] = loadFromXml(XML.load(url))

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