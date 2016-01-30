package com.softwaremill.clippy

import java.util.regex.Pattern

import scala.util.Try
import scala.util.matching.Regex
import scala.xml.{XML, NodeSeq}

sealed trait CompilationError[T <: Template] {
  def toXml: NodeSeq
  def toXmlString: String = toXml.toString()
  def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT): Boolean
  def asRegex(implicit ev: T =:= ExactT): CompilationError[RegexT]
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

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case TypeMismatchError(f, fe, r, re) =>
      def optMatches(t: Option[T], v: Option[ExactT]) = (for {
        tt <- t
        vv <- v
      } yield tt.matches(vv)).getOrElse(true)

      found.matches(f) && optMatches(foundExpandsTo, fe) && required.matches(r) && optMatches(requiredExpandsTo, re)

    case _ =>
      false
  }

  override def asRegex(implicit ev: T =:= ExactT) = TypeMismatchError(
    RegexT.fromPattern(found.v), foundExpandsTo.map(fe => RegexT.fromPattern(fe.v)),
    RegexT.fromPattern(required.v), requiredExpandsTo.map(re => RegexT.fromPattern(re.v)))
}

case class NotFoundError[T <: Template](what: T) extends CompilationError[T] {

  override def toString = s"Not found error: $what"

  override def toXml =
    <notFound>{what.v}</notFound>

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case NotFoundError(w) => what.matches(w)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = NotFoundError(RegexT.fromPattern(what.v))
}

case class NotAMemberError[T <: Template](what: T, notAMemberOf: T) extends CompilationError[T] {

  override def toString = s"Not a member error: $what isn't a member of $notAMemberOf"

  override def toXml =
    <notAMember>
      <what>{what.v}</what>
      <notAMemberOf>{notAMemberOf.v}</notAMemberOf>
    </notAMember>

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case NotAMemberError(w, n) => what.matches(w) && notAMemberOf.matches(n)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = NotAMemberError(RegexT.fromPattern(what.v),
    RegexT.fromPattern(notAMemberOf.v))
}

case class ImplicitNotFoundError[T <: Template](parameter: T, implicitType: T) extends CompilationError[T] {

  override def toString = s"Implicit not found error: for parameter $parameter of type $implicitType"

  override def toXml =
    <implicitNotFound>
      <parameter>{parameter.v}</parameter>
      <implicitType>{implicitType.v}</implicitType>
    </implicitNotFound>

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case ImplicitNotFoundError(p, i) => parameter.matches(p) && implicitType.matches(i)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = ImplicitNotFoundError(RegexT.fromPattern(parameter.v),
    RegexT.fromPattern(implicitType.v))
}

case class DivergingImplicitExpansionError[T <: Template](forType: T, startingWith: T, in: T) extends CompilationError[T] {

  override def toString = s"Diverging implicit expansion error: for type $forType starting with $startingWith in $in"

  override def toXml =
    <divergingImplicitExpansion>
      <forType>{forType.v}</forType>
      <startingWith>{startingWith.v}</startingWith>
      <in>{in.v}</in>
    </divergingImplicitExpansion>

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case DivergingImplicitExpansionError(f, s, i) => forType.matches(f) && startingWith.matches(s) && in.matches(i)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = DivergingImplicitExpansionError(
    RegexT.fromPattern(forType.v), RegexT.fromPattern(startingWith.v), RegexT.fromPattern(in.v))
}

object CompilationError {
  def fromXmlString(s: String): Option[CompilationError[RegexT]] = fromXml(XML.loadString(s))

  def fromXml(xml: NodeSeq): Option[CompilationError[RegexT]] = {
    def extractTypeMismatch =
      (xml \\ "typeMismatch").headOption.map { n =>
        TypeMismatchError(
          RegexT.fromRegex((n \ "found").text),
          (n \ "foundExpandsTo").headOption.map(n => RegexT.fromRegex(n.text)),
          RegexT.fromRegex((n \ "required").text),
          (n \ "requiredExpandsTo").headOption.map(n => RegexT.fromRegex(n.text))
        )
      }

    def extractNotFound =
      (xml \\ "notFound").headOption.map { n =>
        NotFoundError(RegexT.fromRegex(n.text))
      }

    def extractNotAMemberOf =
      (xml \\ "notAMember").headOption.map { n =>
        NotAMemberError(
          RegexT.fromRegex((n \ "what").text),
          RegexT.fromRegex((n \ "notAMemberOf").text)
        )
      }

    def extractImplicitNotFound =
      (xml \\ "implicitNotFound").headOption.map { n =>
        ImplicitNotFoundError(
          RegexT.fromRegex((n \ "parameter").text),
          RegexT.fromRegex((n \ "implicitType").text)
        )
      }

    def extractDivergingImplicitExpansion =
      (xml \\ "divergingImplicitExpansion").headOption.map { n =>
        DivergingImplicitExpansionError(
          RegexT.fromRegex((n \ "forType").text),
          RegexT.fromRegex((n \ "startingWith").text),
          RegexT.fromRegex((n \ "in").text)
        )
      }

    extractTypeMismatch
      .orElse(extractNotFound)
      .orElse(extractNotAMemberOf)
      .orElse(extractImplicitNotFound)
      .orElse(extractDivergingImplicitExpansion)
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
  private val DivergingImplicitExpansionRegexp = """diverging implicit expansion for type\s*([^\s]+)\s*.*\s*starting with method\s*([^\s]+)\s*in\s*([^\n]+)""".r

  def parse(error: String): Option[CompilationError[ExactT]] = {
    if (error.contains("type mismatch")) {
      RequiredPrefixRegexp.split(error).toList match {
        case List(beforeReq, afterReq) =>
          for {
            found <- FoundRegexp.findFirstMatchIn(beforeReq)
            foundExpandsTo = WhichExpandsToRegexp.findFirstMatchIn(beforeReq)
            required <- AfterRequiredRegexp.findFirstMatchIn(error)
            requiredExpandsTo = WhichExpandsToRegexp.findFirstMatchIn(afterReq)
          } yield TypeMismatchError[ExactT](ExactT(found.group(1)), foundExpandsTo.map(m => ExactT(m.group(1))),
            ExactT(required.group(1)), requiredExpandsTo.map(m => ExactT(m.group(1))))

        case _ =>
          None
      }
    }
    else if (error.contains("not found")) {
      for {
        what <- NotFoundRegexp.findFirstMatchIn(error)
      } yield NotFoundError[ExactT](ExactT(what.group(1)))
    }
    else if (error.contains("is not a member of")) {
      for {
        what <- NotAMemberRegexp.findFirstMatchIn(error)
        notAMemberOf <- NotAMemberOfRegexp.findFirstMatchIn(error)
      } yield NotAMemberError[ExactT](ExactT(what.group(1)), ExactT(notAMemberOf.group(1)))
    }
    else if (error.contains("could not find implicit value for parameter")) {
      for {
        inf <- ImplicitNotFoundRegexp.findFirstMatchIn(error)
      } yield ImplicitNotFoundError[ExactT](ExactT(inf.group(1)), ExactT(inf.group(2)))
    }
    else if (error.contains("diverging implicit expansion for type")) {
      for {
        inf <- DivergingImplicitExpansionRegexp.findFirstMatchIn(error)
      } yield DivergingImplicitExpansionError[ExactT](ExactT(inf.group(1)), ExactT(inf.group(2)), ExactT(inf.group(3)))
    }
    else None
  }
}

sealed trait Template {
  def v: String
}

case class ExactT(v: String) extends Template {
  override def toString = v
}

case class RegexT(v: String) extends Template {
  lazy val regex = Try(new Regex(v)).getOrElse(new Regex("^$"))
  def matches(e: ExactT): Boolean = regex.pattern.matcher(e.v).matches()
  override def toString = v
}
object RegexT {
  /**
    * Patterns can include wildcards (`*`)
    */
  def fromPattern(pattern: String): RegexT = {
    val regexp = pattern
      .split("\\*", -1)
      .map(el => if (el != "") Pattern.quote(el) else el)
      .flatMap(el => List(".*", el))
      .tail
      .filter(_.nonEmpty)
      .mkString("")

    RegexT.fromRegex(regexp)
  }

  def fromRegex(v: String): RegexT = {
    new RegexT(v)
  }
}