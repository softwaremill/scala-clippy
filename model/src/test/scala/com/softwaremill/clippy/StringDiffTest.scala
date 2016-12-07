package com.softwaremill.clippy

import com.softwaremill.clippy.StringDiff.{DELTA_END, DELTA_START}
import org.scalatest.{FlatSpec, Matchers}

class StringDiffTest extends FlatSpec with Matchers {

  val testData = List(
    ("Super[String, String]", "Super[Option[String], String]", "expected Super[" + DELTA_START + "String" + DELTA_END + ", String] but was Super[" + DELTA_START + "Option[String]" + DELTA_END + ", String]"),
    ("Cool[String, String]", "Super[Option[String], String]", "expected " + DELTA_START + "Cool[String" + DELTA_END + ", String] but was " + DELTA_START + "Super[Option[String]" + DELTA_END + ", String]"),
    ("(String, String)", "Super[Option[String], String]", "expected " + DELTA_START + "(String, String)" + DELTA_END + " but was " + DELTA_START + "Super[Option[String], String]" + DELTA_END)
  )

  "StringDiff" should "diff" in {
    for ((expected, actual, expectedDiff) <- testData) {

      val diff = new StringDiff(expected, actual).diff("expected %s but was %s")

      diff should equal(expectedDiff)
    }
  }

}
