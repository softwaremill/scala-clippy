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

case class NotAMemberError[T <: Template](what: T, notAMemberOf: T) extends CompilationError[T] {

  override def toString = s"Not a member error: $what isn't a member of $notAMemberOf"

  override def toXml =
    <notAMember>
      <what>{what.v}</what>
      <notAMemberOf>{notAMemberOf.v}</notAMemberOf>
    </notAMember>

  override def matches(other: CompilationError[Exact])(implicit ev: T =:= ExactOrRegex) = other match {
    case NotAMemberError(w, n) => what.matches(w) && notAMemberOf.matches(n)
    case _ => false
  }

  override def asExactOrRegex(implicit ev: T =:= Exact) = NotAMemberError(ExactOrRegex(what.v), ExactOrRegex(notAMemberOf.v))
}

case class ImplicitNotFound[T <: Template](parameter: T, implicitType: T) extends CompilationError[T] {

  override def toString = s"Implicit not found error: for parameter $parameter of type $implicitType"

  override def toXml =
    <implicitNotFound>
      <parameter>{parameter.v}</parameter>
      <implicitType>{implicitType.v}</implicitType>
    </implicitNotFound>

  override def matches(other: CompilationError[Exact])(implicit ev: T =:= ExactOrRegex) = other match {
    case ImplicitNotFound(p, i) => parameter.matches(p) && implicitType.matches(i)
    case _ => false
  }

  override def asExactOrRegex(implicit ev: T =:= Exact) = ImplicitNotFound(ExactOrRegex(parameter.v), ExactOrRegex(implicitType.v))
}

object CompilationError {
  def fromXmlString(s: String): Option[CompilationError[ExactOrRegex]] = fromXml(XML.loadString(s))

  def fromXml(xml: NodeSeq): Option[CompilationError[ExactOrRegex]] = {
    def extractTypeMismatch =
      (xml \\ "typeMismatch").headOption.map { n =>
        TypeMismatchError(
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

    def extractNotAMemberOf =
      (xml \\ "notAMember").headOption.map { n =>
        NotAMemberError(
          ExactOrRegex((n \ "what").text),
          ExactOrRegex((n \ "notAMemberOf").text)
        )
      }

    def extractImplicitNotFound =
      (xml \\ "implicitNotFound").headOption.map { n =>
        ImplicitNotFound(
          ExactOrRegex((n \ "parameter").text),
          ExactOrRegex((n \ "implicitType").text)
        )
      }

    extractTypeMismatch
      .orElse(extractNotFound)
      .orElse(extractNotAMemberOf)
      .orElse(extractImplicitNotFound)
  }
}

object CompilationErrorParser {
  private val FoundRegexp = """found\s*:\s*([^\n]+)\n""".r
  private val RequiredPrefixRegexp = """required\s*:""".r
  private val AfterRequiredRegexp = """required\s*:\s*([^\n]+)""".r
  private val WhichExpandsToRegexp = """\s*\(which expands to\)\s*([^\n]+)""".r
  private val NotFoundRegexp = """not found\s*:\s*([^\n]+)""".r
  private val NotAMemberRegexp = """:?\s*([^\n:]+) is not a member of""".r
  private val NotAMemberOfRegexp = """is not a member of\s*([^\n]+)""".r
  private val ImplicitNotFoundRegexp = """could not find implicit value for parameter\s*([^:]+):\s*([^\n]+)""".r

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
    else if (error.contains("is not a member of")) {
      for {
        what <- NotAMemberRegexp.findFirstMatchIn(error)
        notAMemberOf <- NotAMemberOfRegexp.findFirstMatchIn(error)
      } yield NotAMemberError[Exact](Exact(what.group(1)), Exact(notAMemberOf.group(1)))
    }
    else if (error.contains("could not find implicit value for parameter")) {
      for {
        inf <- ImplicitNotFoundRegexp.findFirstMatchIn(error)
      } yield ImplicitNotFound[Exact](Exact(inf.group(1)), Exact(inf.group(2)))
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