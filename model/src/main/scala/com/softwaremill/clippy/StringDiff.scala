package com.softwaremill.clippy

object StringDiff {
  val separators                       = List(' ', ',', '(', ')', '[', ']', '#', '#', '=', '>', '{', '.')
  def isSeparator(char: Char): Boolean = separators.contains(char)
}

class StringDiff(expected: String, actual: String, color: String => String) {
  import StringDiff._
  def diff(message: String): String =
    if (this.expected == this.actual) format(message, this.expected, this.actual)
    else format(message, markDiff(expected), markDiff(actual))

  private def format(msg: String, expected: String, actual: String) = msg.format(expected, actual)

  private def markDiff(source: String) = {
    val prefix = findCommonPrefix()
    val suffix = findCommonSuffix()
    if (overlappingPrefixSuffix(source, prefix, suffix))
      source
    else {
      val diff = color(source.substring(prefix.length, source.length - suffix.length))
      prefix + diff + suffix
    }
  }

  private def overlappingPrefixSuffix(source: String, prefix: String, suffix: String) =
    prefix.length + suffix.length >= source.length

  def findCommonPrefix(expectedStr: String = expected, actualStr: String = actual): String = {
    val prefixChars = expectedStr.zip(actualStr).takeWhile(Function.tupled(_ == _)).map(_._1)

    val lastSeparatorIndex = prefixChars.lastIndexWhere(isSeparator)
    val prefixEndIndex     = if (lastSeparatorIndex == -1) 0 else lastSeparatorIndex + 1

    if (prefixChars.nonEmpty && prefixEndIndex < prefixChars.length)
      prefixChars.mkString.substring(0, prefixEndIndex)
    else {
      prefixChars.mkString
    }
  }

  def findCommonSuffix(): String =
    findCommonPrefix(expected.reverse, actual.reverse).reverse
}
