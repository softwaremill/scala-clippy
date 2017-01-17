package com.softwaremill.clippy

import com.softwaremill.clippy.StringDiff.{DeltaEnd, DeltaStart}
import org.scalatest.{FlatSpec, Matchers}

class StringDiffTest extends FlatSpec with Matchers {

  val testData = List(
    ("Super[String, String]", "Super[Option[String], String]", "expected Super[" + DeltaStart + "String" + DeltaEnd + ", String] but was Super[" + DeltaStart + "Option[String]" + DeltaEnd + ", String]"),
    ("Cool[String, String]", "Super[Option[String], String]", "expected " + DeltaStart + "Cool[String" + DeltaEnd + ", String] but was " + DeltaStart + "Super[Option[String]" + DeltaEnd + ", String]"),
    ("(String, String)", "Super[Option[String], String]", "expected " + DeltaStart + "(String, String)" + DeltaEnd + " but was " + DeltaStart + "Super[Option[String], String]" + DeltaEnd),
    ("Map[Long, Double]", "Map[String, Double]", "expected Map[" + DeltaStart + "Long" + DeltaEnd + ", Double] but was Map[" + DeltaStart + "String" + DeltaEnd + ", Double]")
  )

  "StringDiff" should "diff" in {
    for ((expected, actual, expectedDiff) <- testData) {

      val diff = new StringDiff(expected, actual).diff("expected %s but was %s")

      diff should be(expectedDiff)
    }
  }

  it should "be considered empty for equal expected and actal texts" in {
    new StringDiff("None", "None") should be('empty)
  }

  it should "find common prefix" in {
    new StringDiff("Map[Long, Double]", "Map[String, Double]").findCommonPrefix() should be("Map[")
  }

  it should "find common suffix" in {
    new StringDiff("Map[Long, Double]", "Map[String, Double]").findCommonSuffix() should be(", Double]")
  }
}
