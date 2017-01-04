package com.softwaremill.clippy

import com.softwaremill.clippy.StringDiff.{DeltaEnd, DeltaStart}
import org.scalatest.{FlatSpec, Matchers}

class StringDiffTest extends FlatSpec with Matchers {

  val testData = List(
    ("Super[String, String]", "Super[Option[String], String]", "expected Super[" + DeltaStart + "String" + DeltaEnd + ", String] but was Super[" + DeltaStart + "Option[String]" + DeltaEnd + ", String]"),
    ("Cool[String, String]", "Super[Option[String], String]", "expected " + DeltaStart + "Cool[String" + DeltaEnd + ", String] but was " + DeltaStart + "Super[Option[String]" + DeltaEnd + ", String]"),
    ("(String, String)", "Super[Option[String], String]", "expected " + DeltaStart + "(String, String)" + DeltaEnd + " but was " + DeltaStart + "Super[Option[String], String]" + DeltaEnd)
  )

  "StringDiff" should "diff" in {
    for ((expected, actual, expectedDiff) <- testData) {

      val diff = new StringDiff(expected, actual).diff("expected %s but was %s")

      diff should be(expectedDiff)
    }
  }

}
