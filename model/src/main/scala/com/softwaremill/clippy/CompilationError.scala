package com.softwaremill.clippy

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