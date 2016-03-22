package com.softwaremill.clippy

import java.util.regex.Pattern

import scala.util.Try
import scala.util.matching.Regex

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

  def setMatches(rr: Set[RegexT], ee: Set[ExactT]): Boolean = {
    if (rr.size != ee.size) false else {
      rr.toList.forall { r => ee.exists(r.matches) }
    }
  }
}
