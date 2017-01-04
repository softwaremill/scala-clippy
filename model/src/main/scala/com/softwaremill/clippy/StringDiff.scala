package com.softwaremill.clippy

import com.softwaremill.clippy.StringDiff.{DeltaEnd, DeltaStart}

object StringDiff {
  val AnsiReset = "\u001B[0m"
  val AnsiRed = "\u001B[31m"
  val DeltaEnd = AnsiReset
  val DeltaStart = AnsiRed
}

class StringDiff(expected: String, actual: String) {

  def diff(message: String): String = {
    if (this.expected == this.actual) format(message, this.expected, this.actual)
    else format(message, markDiff(expected), markDiff(actual))
  }

  private def format(msg: String, expected: String, actual: String) = msg.format(expected, actual)

  private def markDiff(source: String) = {
    val prefix = findCommonPrefix()
    val suffix = findCommonSuffix()
    val diff = DeltaStart + source.substring(prefix.length, source.length - suffix.length) + DeltaEnd
    prefix + diff + suffix
  }

  private def findCommonPrefix(expectedStr: String = expected, actualStr: String = actual): String = {
    expectedStr.zip(actualStr).takeWhile(Function.tupled(_ == _)).map(_._1).mkString
  }

  private def findCommonSuffix(): String =
    findCommonPrefix(expected.reverse, actual.reverse).reverse
}
