package org.softwaremill.clippy

import java.net.URL

import scala.xml._

sealed trait Advice
case class TypeMismatchAdvice(found: String, required: String, advice: String) extends Advice

object Advices {
  def loadFromClasspath(): List[Advice] = {
    val e = this.getClass.getClassLoader.getResources("clippy.xml")
    var r: List[Advice] = Nil
    while (e.hasMoreElements) {
      r ++= loadFromURL(e.nextElement())
    }
    r
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