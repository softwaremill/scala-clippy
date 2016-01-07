package com.softwaremill.clippy

import scala.xml.{XML, NodeSeq}

sealed trait CompilationError {
  def toXml: NodeSeq
  def toXmlString: String = toXml.toString()
}
case class TypeMismatchError(found: String, required: String) extends CompilationError {
  override def toString = s"Type mismatch error.\nFound: $found,\nrequired: $required"
  override def toXml =
    <typemismatch>
      <found>${found}</found>
      <required>${required}</required>
    </typemismatch>
}
case class NotFoundError(what: String) extends CompilationError {
  override def toString = s"Not found error: $what"
  override def toXml =
    <notfound>${what}</notfound>
}

object CompilationError {
  def fromXmlString(s: String): Option[CompilationError] = fromXml(XML.loadString(s))

  def fromXml(xml: NodeSeq): Option[CompilationError] = {
    def extractTypeMismatch =
      (xml \ "typemismatch").headOption.map { n =>
        TypeMismatchError(
          (n \ "found").text,
          (n \ "required").text
        )
      }

    def extractNotFound =
      (xml \ "notfound").headOption.map { n =>
      NotFoundError(n.text)
    }

    extractTypeMismatch
      .orElse(extractNotFound)
  }
}

object CompilationErrorParser {
  private val FoundRegexp = """found\s*:\s*([^\n]+)\n""".r
  private val RequiredRegexp = """required\s*:\s*(.+)""".r
  private val NotFoundRegexp = """not found\s*:\s*(.+)""".r

  def parse(error: String): Option[CompilationError] = {
    if (error.startsWith("type mismatch")) {
      for {
        found <- FoundRegexp.findFirstMatchIn(error)
        required <- RequiredRegexp.findFirstMatchIn(error)
      } yield TypeMismatchError(found.group(1), required.group(1))
    }
    else if (error.startsWith("not found")) {
      for {
        what <- NotFoundRegexp.findFirstMatchIn(error)
      } yield NotFoundError(what.group(1))
    }
    else None
  }
}
