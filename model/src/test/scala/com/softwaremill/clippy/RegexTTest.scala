package com.softwaremill.clippy

import org.scalatest.{Matchers, FlatSpec}

class RegexTTest extends FlatSpec with Matchers {
  val matchingTests = List(
    ("slick.dbio.DBIOAction[*]", "slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]"),
    ("slick.dbio.DBIOAction[Unit,*,*]", "slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]"),
    ("slick.dbio.DBIOAction[*,slick.dbio.NoStream,*]", "slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]")
  )

  val nonMatchingTests = List(
    ("slick.dbio.DBIOAction[*]", "scala.concurrent.Future[Unit]"),
    ("slick.dbio.DBIOAction[Unit,*,*]", "slick.dbio.DBIOAction[String,slick.dbio.NoStream,slick.dbio.Effect.Write]"),
    ("slick.dbio.DBIOAction[*,slick.dbio.NoStream,*]", "slick.dbio.DBIOAction[Unit,slick.dbio.Stream,slick.dbio.Effect.Write]")
  )

  for ((pattern, test) <- matchingTests) {
    pattern should s"match $test" in {
      RegexT.fromPattern(pattern).matches(ExactT(test)) should be (true)
    }
  }

  for ((pattern, test) <- nonMatchingTests) {
    pattern should s"not match $test" in {
      RegexT.fromPattern(pattern).matches(ExactT(test)) should be (false)
    }
  }
}
