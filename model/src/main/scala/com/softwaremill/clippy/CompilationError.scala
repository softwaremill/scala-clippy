package com.softwaremill.clippy

import org.json4s.JValue
import org.json4s.JsonAST.{JField, JString, JObject}
import org.json4s.native.JsonMethods._

import CompilationError._

sealed trait CompilationError[T <: Template] {
  def toJson: JValue
  def toJsonString: String = compact(render(toJson))
  def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT): Boolean
  def asRegex(implicit ev: T =:= ExactT): CompilationError[RegexT]
}

case class TypeMismatchError[T <: Template](found: T, foundExpandsTo: Option[T],
  required: T, requiredExpandsTo: Option[T]) extends CompilationError[T] {

  override def toString = {
    def expandsTo(et: Option[T]): String = et.fold("")(e => s" (expands to: $e)")
    s"Type mismatch error.\nFound: $found${expandsTo(foundExpandsTo)},\nrequired: $required${expandsTo(requiredExpandsTo)}"
  }

  override def toJson =
    JObject(
      List(
        TypeField -> JString("typeMismatch"),
        "found" -> JString(found.v),
        "required" -> JString(required.v))
        ++ foundExpandsTo.fold[List[JField]](Nil)(e => List("foundExpandsTo" -> JString(e.v)))
        ++ requiredExpandsTo.fold[List[JField]](Nil)(e => List("requiredExpandsTo" -> JString(e.v)))
    )

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

  override def toJson =
    JObject(TypeField -> JString("notFound"), "what" -> JString(what.v))

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case NotFoundError(w) => what.matches(w)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = NotFoundError(RegexT.fromPattern(what.v))
}

case class NotAMemberError[T <: Template](what: T, notAMemberOf: T) extends CompilationError[T] {

  override def toString = s"Not a member error: $what isn't a member of $notAMemberOf"

  override def toJson =
    JObject(
      TypeField -> JString("notAMember"),
      "what" -> JString(what.v),
      "notAMemberOf" -> JString(notAMemberOf.v)
    )

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case NotAMemberError(w, n) => what.matches(w) && notAMemberOf.matches(n)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = NotAMemberError(RegexT.fromPattern(what.v),
    RegexT.fromPattern(notAMemberOf.v))
}

case class ImplicitNotFoundError[T <: Template](parameter: T, implicitType: T) extends CompilationError[T] {

  override def toString = s"Implicit not found error: for parameter $parameter of type $implicitType"

  override def toJson =
    JObject(
      TypeField -> JString("implicitNotFound"),
      "parameter" -> JString(parameter.v),
      "implicitType" -> JString(implicitType.v)
    )

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case ImplicitNotFoundError(p, i) => parameter.matches(p) && implicitType.matches(i)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = ImplicitNotFoundError(RegexT.fromPattern(parameter.v),
    RegexT.fromPattern(implicitType.v))
}

case class DivergingImplicitExpansionError[T <: Template](forType: T, startingWith: T, in: T) extends CompilationError[T] {

  override def toString = s"Diverging implicit expansion error: for type $forType starting with $startingWith in $in"

  override def toJson =
    JObject(
      TypeField -> JString("divergingImplicitExpansion"),
      "forType" -> JString(forType.v),
      "startingWith" -> JString(startingWith.v),
      "in" -> JString(in.v)
    )

  override def matches(other: CompilationError[ExactT])(implicit ev: T =:= RegexT) = other match {
    case DivergingImplicitExpansionError(f, s, i) => forType.matches(f) && startingWith.matches(s) && in.matches(i)
    case _ => false
  }

  override def asRegex(implicit ev: T =:= ExactT) = DivergingImplicitExpansionError(
    RegexT.fromPattern(forType.v), RegexT.fromPattern(startingWith.v), RegexT.fromPattern(in.v))
}

object CompilationError {
  val TypeField = "type"

  def fromJsonString(s: String): Option[CompilationError[RegexT]] = fromJson(parse(s))

  def fromJson(jvalue: JValue): Option[CompilationError[RegexT]] = {

    def regexTFromJson(fields: List[JField], name: String): Option[RegexT] =
      (for {
        JField(`name`, JString(v)) <- fields
      } yield RegexT.fromRegex(v)).headOption

    def extractWithType(typeValue: String, fields: List[JField]): Option[CompilationError[RegexT]] = typeValue match {
      case "typeMismatch" =>
        for {
          found <- regexTFromJson(fields, "found")
          foundExpandsTo = regexTFromJson(fields, "foundExpandsTo")
          required <- regexTFromJson(fields, "required")
          requiredExpandsTo = regexTFromJson(fields, "requiredExpandsTo")
        } yield TypeMismatchError(found, foundExpandsTo, required, requiredExpandsTo)

      case "notFound" =>
        for {
          what <- regexTFromJson(fields, "what")
        } yield NotFoundError(what)

      case "notAMember" =>
        for {
          what <- regexTFromJson(fields, "what")
          notAMemberOf <- regexTFromJson(fields, "notAMemberOf")
        } yield NotAMemberError(what, notAMemberOf)

      case "implicitNotFound" =>
        for {
          parameter <- regexTFromJson(fields, "parameter")
          implicitType <- regexTFromJson(fields, "implicitType")
        } yield ImplicitNotFoundError(parameter, implicitType)

      case "divergingImplicitExpansion" =>
        for {
          forType <- regexTFromJson(fields, "forType")
          startingWith <- regexTFromJson(fields, "startingWith")
          in <- regexTFromJson(fields, "in")
        } yield DivergingImplicitExpansionError(forType, startingWith, in)
    }

    (for {
      JObject(fields) <- jvalue
      JField(TypeField, JString(typeValue)) <- fields
      v <- extractWithType(typeValue, fields).toList
    } yield v).headOption
  }
}
