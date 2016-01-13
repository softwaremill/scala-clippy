package com.softwaremill.clippy

import org.scalacheck.Prop._
import org.scalacheck.Properties
import org.scalatest.{FlatSpec, Matchers}

class TypeMismatchAdviceTest extends FlatSpec with Matchers {

  behavior of "Type Mismatch Advice"

  it should "match exact expression" in {
    // given
    val advice = new TypeMismatchAdvice("com.softwaremill.String", "com.softwaremill.RequiredType[String]", "adviceText")
    val matchingErr = TypeMismatchError("com.softwaremill.String", None, "com.softwaremill.RequiredType[String]", None)
    val nonMatchingErrs = List(
      new TypeMismatchError("com.softwaremill.String", None, "com.softwaremill.OtherType", None),
      new TypeMismatchError("com.softwaremill.Int", None, "com.softwaremill.OtherType", None),
      new TypeMismatchError("com.softwaremill.Int", None, "com.softwaremill.RequiredType[String]", None)
    )

    // then
    advice.errMatching should be definedAt matchingErr
    nonMatchingErrs.foreach(advice.errMatching should not be definedAt(_))
  }

  it should "match regexes" in {
    // given
    val advice = new TypeMismatchAdvice("slick.dbio.DBIOAction\\[.*\\]", "slick.lifted.Rep\\[Option\\[.*\\]\\]", "adviceText")
    val matchingErrs = List(
      TypeMismatchError("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]", None, "slick.lifted.Rep[Option[?]]", None),
      TypeMismatchError("slick.dbio.DBIOAction[String,slick.dbio.NoStream,slick.dbio.Effect.Read]", None, "slick.lifted.Rep[Option[Int]]", None),
      TypeMismatchError("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]", None, "slick.lifted.Rep[Option[Option[Int]]", None)
    )
    val nonMatchingErrs = List(
      TypeMismatchError("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]", None, "String", None),
      TypeMismatchError("String", None, "slick.lifted.Rep[Option[?]]", None),
      TypeMismatchError("String", None, "com.softwaremill.AweSomeType", None)
    )

    // then
    matchingErrs.foreach(advice.errMatching should be definedAt (_))
    nonMatchingErrs.foreach(advice.errMatching should not be definedAt(_))
  }
}

class TypeMismatchAdviceProperties extends Properties("TypeMismatch advice") {
  property("matches identical strings") = forAll { (found: String, required: String) =>
    TypeMismatchAdvice(found, required, "Try again later.").errMatching.isDefinedAt(TypeMismatchError(found, None, required, None))
  }
}