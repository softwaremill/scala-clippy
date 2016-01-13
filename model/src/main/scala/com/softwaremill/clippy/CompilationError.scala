package com.softwaremill.clippy

import scala.xml.{XML, NodeSeq}

sealed trait CompilationError {
  def toXml: NodeSeq
  def toXmlString: String = toXml.toString()
}
case class TypeMismatchError(found: String, foundExpandsTo: Option[String],
  required: String, requiredExpandsTo: Option[String]) extends CompilationError {

  override def toString = {
    def expandsTo(et: Option[String]): String = et.fold("")(e => s" (expands to: $e)")
    s"Type mismatch error.\nFound: $found${expandsTo(foundExpandsTo)},\nrequired: $required${expandsTo(requiredExpandsTo)}"
  }
  override def toXml =
    <typemismatch>
      <found>{found}</found>
      {foundExpandsTo.fold(NodeSeq.Empty)(e => <foundExpandsTo>{e}</foundExpandsTo>)}
      <required>{required}</required>
      {requiredExpandsTo.fold(NodeSeq.Empty)(e => <requiredExpandsTo>{e}</requiredExpandsTo>)}
    </typemismatch>
}
case class NotFoundError(what: String) extends CompilationError {
  override def toString = s"Not found error: $what"
  override def toXml =
    <notfound>{what}</notfound>
}

object CompilationError {
  def fromXmlString(s: String): Option[CompilationError] = fromXml(XML.loadString(s))

  def fromXml(xml: NodeSeq): Option[CompilationError] = {
    def extractTypeMismatch =
      (xml \\ "typemismatch").headOption.map { n =>
        TypeMismatchError(
          (n \ "found").text,
          (n \ "foundExpandsTo").headOption.map(_.text),
          (n \ "required").text,
          (n \ "requiredExpandsTo").headOption.map(_.text)
        )
      }

    def extractNotFound =
      (xml \\ "notfound").headOption.map { n =>
      NotFoundError(n.text)
    }

    extractTypeMismatch
      .orElse(extractNotFound)
  }
}

object CompilationErrorParser {
  private val FoundRegexp = """found\s*:\s*([^\n]+)\n""".r
  private val RequiredPrefixRegexp = """required\s*:""".r
  private val AfterRequiredRegexp = """required\s*:\s*([^\n]+)""".r
  private val WhichExpandsToRegexp = """\s*\(which expands to\)\s*([^\n]+)""".r
  private val NotFoundRegexp = """not found\s*:\s*([^\n]+)""".r

  def parse(error: String): Option[CompilationError] = {
    if (error.contains("type mismatch")) {
      RequiredPrefixRegexp.split(error).toList match {
        case List(beforeReq, afterReq) =>
          for {
            found <- FoundRegexp.findFirstMatchIn(beforeReq)
            foundExpandsTo = WhichExpandsToRegexp.findFirstMatchIn(beforeReq)
            required <- AfterRequiredRegexp.findFirstMatchIn(error)
            requiredExpandsTo = WhichExpandsToRegexp.findFirstMatchIn(afterReq)
          } yield TypeMismatchError(found.group(1), foundExpandsTo.map(_.group(1)),
            required.group(1), requiredExpandsTo.map(_.group(1)))

        case _ =>
          None
      }
    }
    else if (error.contains("not found")) {
      for {
        what <- NotFoundRegexp.findFirstMatchIn(error)
      } yield NotFoundError(what.group(1))
    }
    else None
  }
}
