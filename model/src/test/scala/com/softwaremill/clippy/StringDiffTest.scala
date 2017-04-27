package com.softwaremill.clippy

import org.scalatest.{FlatSpec, Matchers}

class StringDiffTest extends FlatSpec with Matchers {

  val S     = "S"
  val E     = "E"
  val AddSE = (s: String) => S + s + E

  val testData = List(
    (
      "Super[String, String]",
      "Super[Option[String], String]",
      "expected Super[" + S + "String" + E + ", String] but was Super[" + S + "Option[String]" + E + ", String]"
    ),
    (
      "Cool[String, String]",
      "Super[Option[String], String]",
      "expected " + S + "Cool[String" + E + ", String] but was " + S + "Super[Option[String]" + E + ", String]"
    ),
    (
      "(String, String)",
      "Super[Option[String], String]",
      "expected " + S + "(String, String)" + E + " but was " + S + "Super[Option[String], String]" + E
    ),
    (
      "Map[Long, Double]",
      "Map[String, Double]",
      "expected Map[" + S + "Long" + E + ", Double] but was Map[" + S + "String" + E + ", Double]"
    ),
    (
      "(Int, Int, Float, Int, Char)",
      "(Int, Int, Int, Char)",
      "expected (Int, Int, " + S + "Float" + E + ", Int, Char) but was (Int, Int, Int, Char)"
    )
  )

  "StringDiff" should "diff" in {
    for ((expected, actual, expectedDiff) <- testData) {

      val diff = new StringDiff(expected, actual, AddSE).diff("expected %s but was %s")

      diff should be(expectedDiff)
    }
  }

  it should "find common prefix" in {
    new StringDiff("Map[Long, Double]", "Map[String, Double]", AddSE).findCommonPrefix() should be("Map[")
  }

  it should "find common suffix" in {
    new StringDiff("Map[Long, Double]", "Map[String, Double]", AddSE).findCommonSuffix() should be(", Double]")
  }
}
