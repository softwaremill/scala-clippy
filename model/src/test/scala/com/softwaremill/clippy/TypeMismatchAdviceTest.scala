package com.softwaremill.clippy

import org.scalacheck.Prop._
import org.scalacheck.Properties
import org.scalatest.{FlatSpec, Matchers}

class TypeMismatchAdviceTest extends FlatSpec with Matchers {

  behavior of "Type Mismatch Advice"

  it should "match exact expression" in {
    // given
    val advice = new TypeMismatchAdvice("com.softwaremill.String", "com.softwaremill.RequiredType[String]", "adviceText")
    val matchingErr = TypeMismatchError("com.softwaremill.String", "com.softwaremill.RequiredType[String]")
    val nonMatchingErrs = List(
      new TypeMismatchError("com.softwaremill.String", "com.softwaremill.OtherType"),
      new TypeMismatchError("com.softwaremill.Int", "com.softwaremill.OtherType"),
      new TypeMismatchError("com.softwaremill.Int", "com.softwaremill.RequiredType[String]")
    )

    // then
    advice.errMatching should be definedAt matchingErr
    nonMatchingErrs.foreach(advice.errMatching should not be definedAt(_))
  }

  it should "match regexes" in {
    // given
    val advice = new TypeMismatchAdvice("slick.dbio.DBIOAction\\[.*\\]", "slick.lifted.Rep\\[Option\\[.*\\]\\]", "adviceText")
    val matchingErrs = List(
      TypeMismatchError("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]", "slick.lifted.Rep[Option[?]]"),
      TypeMismatchError("slick.dbio.DBIOAction[String,slick.dbio.NoStream,slick.dbio.Effect.Read]", "slick.lifted.Rep[Option[Int]]"),
      TypeMismatchError("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]", "slick.lifted.Rep[Option[Option[Int]]")
    )
    val nonMatchingErrs = List(
      TypeMismatchError("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]", "String"),
      TypeMismatchError("String", "slick.lifted.Rep[Option[?]]"),
      TypeMismatchError("String", "com.softwaremill.AweSomeType")
    )

    // then
    matchingErrs.foreach(advice.errMatching should be definedAt (_))
    nonMatchingErrs.foreach(advice.errMatching should not be definedAt(_))
  }
}

class TypeMismatchAdviceProperties extends Properties("TypeMismatch advice") {
  property("matches identical strings") = forAll { (found: String, required: String) =>
    TypeMismatchAdvice(found, required, "Try again later.").errMatching.isDefinedAt(TypeMismatchError(found, required))
  }
}