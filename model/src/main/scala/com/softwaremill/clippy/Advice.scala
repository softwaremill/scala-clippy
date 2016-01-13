package com.softwaremill.clippy

import scala.util.Try
import scala.util.matching.Regex

sealed trait Advice extends RegexSupport {
  def errMatching: PartialFunction[CompilationError, String]
}

case class NotFoundAdvice(what: String, adviceText: String) extends Advice {
  val whatRegex = compileRegex(what)

  override def errMatching = {
    case NotFoundError(errWhat) if equalsOrMatches(errWhat, what, whatRegex) => adviceText
  }
}

case class TypeMismatchAdvice(found: String, required: String, adviceText: String) extends Advice {
  val foundRegex = compileRegex(found)
  val requiredRegex = compileRegex(required)

  override def errMatching = {
    case TypeMismatchError(errFound, _, errRequired, _) if equalsOrMatches(errFound, found, foundRegex) &&
      equalsOrMatches(errRequired, required, requiredRegex) => adviceText
  }
}

private[clippy] trait RegexSupport {
  protected def equalsOrMatches(actual: String, expected: String, expectedRegex: Regex): Boolean =
    actual == expected || expectedRegex.pattern.matcher(actual).matches()

  protected def compileRegex(str: String): Regex = Try(new Regex(str)).getOrElse(new Regex("^$"))
}
