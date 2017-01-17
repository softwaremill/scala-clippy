package com.softwaremill.clippy

object StringDiff {
  val DeltaEnd = Console.RESET
  val DeltaStart = Console.RED
  val separators = List(' ', ',', '(', ')', '[', ']', '#', '#', '=', '>', '{')
  def isSeparator(char: Char) = separators.contains(char)
}

class StringDiff(expected: String, actual: String) {
  import StringDiff._
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

  def findCommonPrefix(expectedStr: String = expected, actualStr: String = actual): String = {
    val prefixChars = expectedStr.zip(actualStr).takeWhile(Function.tupled(_ == _)).map(_._1)

    val lastSeparatorIndex = prefixChars.lastIndexWhere(isSeparator)
    val prefixStartIndex = if (lastSeparatorIndex == -1) 0 else lastSeparatorIndex + 1

    if (prefixChars.nonEmpty && prefixStartIndex < prefixChars.length)
      prefixChars.mkString.substring(0, prefixStartIndex)
    else {
      prefixChars.mkString
    }
  }

  def findCommonSuffix(): String =
    findCommonPrefix(expected.reverse, actual.reverse).reverse
}
