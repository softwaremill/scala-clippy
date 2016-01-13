package com.softwaremill.clippy

import scala.util.Try
import scala.util.matching.Regex
import scala.xml.{XML, NodeSeq}

sealed trait CompilationError[T <: Template] {
  def toXml: NodeSeq
  def toXmlString: String = toXml.toString()
  def matches(other: CompilationError[Exact])(implicit ev: T =:= ExactOrRegex): Boolean
  def asExactOrRegex(implicit ev: T =:= Exact): CompilationError[ExactOrRegex]
}
case class TypeMismatchError[T <: Template](found: T, foundExpandsTo: Option[T],
  required: T, requiredExpandsTo: Option[T]) extends CompilationError[T] {

  override def toString = {
    def expandsTo(et: Option[T]): String = et.fold("")(e => s" (expands to: $e)")
    s"Type mismatch error.\nFound: $found${expandsTo(foundExpandsTo)},\nrequired: $required${expandsTo(requiredExpandsTo)}"
  }

  override def toXml =
    <typeMismatch>
      <found>{found.v}</found>
      {foundExpandsTo.fold(NodeSeq.Empty)(e => <foundExpandsTo>{e.v}</foundExpandsTo>)}
      <required>{required.v}</required>
      {requiredExpandsTo.fold(NodeSeq.Empty)(e => <requiredExpandsTo>{e.v}</requiredExpandsTo>)}
    </typeMismatch>

  override def matches(other: CompilationError[Exact])(implicit ev: T =:= ExactOrRegex) = other match {
    case TypeMismatchError(f, fe, r, re) =>
      def optMatches(t: Option[T], v: Option[Exact]) = (for {
        tt <- t
        vv <- v
      } yield tt.matches(vv)).getOrElse(true)

      found.matches(f) && optMatches(foundExpandsTo, fe) && required.matches(r) && optMatches(requiredExpandsTo, re)

    case _ =>
      false
  }

  override def asExactOrRegex(implicit ev: T =:= Exact) = TypeMismatchError(
    ExactOrRegex(found.v), foundExpandsTo.map(fe => ExactOrRegex(fe.v)),
    ExactOrRegex(required.v), requiredExpandsTo.map(re => ExactOrRegex(re.v)))
}

case class NotFoundError[T <: Template](what: T) extends CompilationError[T] {

  override def toString = s"Not found error: $what"

  override def toXml =
    <notFound>{what.v}</notFound>

  override def matches(other: CompilationError[Exact])(implicit ev: T =:= ExactOrRegex) = other match {
    case NotFoundError(w) => what.matches(w)
    case _ => false
  }

  override def asExactOrRegex(implicit ev: T =:= Exact) = NotFoundError(ExactOrRegex(what.v))
}

object CompilationError {
  def fromXmlString(s: String): Option[CompilationError[ExactOrRegex]] = fromXml(XML.loadString(s))

  def fromXml(xml: NodeSeq): Option[CompilationError[ExactOrRegex]] = {
    def extractTypeMismatch =
      (xml \\ "typeMismatch").headOption.map { n =>
        TypeMismatchError[ExactOrRegex](
          ExactOrRegex((n \ "found").text),
          (n \ "foundExpandsTo").headOption.map(n => ExactOrRegex(n.text)),
          ExactOrRegex((n \ "required").text),
          (n \ "requiredExpandsTo").headOption.map(n => ExactOrRegex(n.text))
        )
      }

    def extractNotFound =
      (xml \\ "notFound").headOption.map { n =>
        NotFoundError(ExactOrRegex(n.text))
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

  def parse(error: String): Option[CompilationError[Exact]] = {
    if (error.contains("type mismatch")) {
      RequiredPrefixRegexp.split(error).toList match {
        case List(beforeReq, afterReq) =>
          for {
            found <- FoundRegexp.findFirstMatchIn(beforeReq)
            foundExpandsTo = WhichExpandsToRegexp.findFirstMatchIn(beforeReq)
            required <- AfterRequiredRegexp.findFirstMatchIn(error)
            requiredExpandsTo = WhichExpandsToRegexp.findFirstMatchIn(afterReq)
          } yield TypeMismatchError[Exact](Exact(found.group(1)), foundExpandsTo.map(m => Exact(m.group(1))),
            Exact(required.group(1)), requiredExpandsTo.map(m => Exact(m.group(1))))

        case _ =>
          None
      }
    }
    else if (error.contains("not found")) {
      for {
        what <- NotFoundRegexp.findFirstMatchIn(error)
      } yield NotFoundError[Exact](Exact(what.group(1)))
    }
    else None
  }
}

sealed trait Template {
  def v: String
}
case class Exact(v: String) extends Template {
  override def toString = v
}
case class ExactOrRegex(v: String) extends Template {
  private lazy val regex = Try(new Regex(v)).getOrElse(new Regex("^$"))
  def matches(e: Exact): Boolean = v == e.v || regex.pattern.matcher(e.v).matches()
  override def toString = v
}