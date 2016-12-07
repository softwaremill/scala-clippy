package com.softwaremill.clippy

import com.softwaremill.clippy.StringDiff.{DELTA_END, DELTA_START}

object StringDiff {
  val ANSI_RESET = "\u001B[0m"
  val ANSI_RED = "\u001B[31m"
  val DELTA_END = ANSI_RESET
  val DELTA_START = ANSI_RED
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
    val diff = DELTA_START + source.substring(prefix.length, source.length - suffix.length) + DELTA_END
    prefix + diff + suffix
  }

  private def findCommonPrefix(): String = {
    expected.zip(actual).takeWhile(Function.tupled(_ == _)).map(_._1).mkString
  }

  private def findCommonSuffix(): String = {
    val reverse = expected.reverse.zip(actual.reverse)
    reverse.takeWhile(Function.tupled(_ == _)).map(_._1).reverse.mkString
  }
}
