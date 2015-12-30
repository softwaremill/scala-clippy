package org.softwaremill.clippy

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
    val advice = new TypeMismatchAdvice(".*g", "com\\.softwaremill.*\\.SomeType", "adviceText")
    val matchingErrs = List(
      TypeMismatchError("String", "com.softwaremill.domain.SomeType"),
      TypeMismatchError("com.softwaremill.wrong.Dog", "com.softwaremill.dto.SomeType"),
      TypeMismatchError("String", "com.softwaremill.SomeType")
    )
    val nonMatchingErrs = List(
      TypeMismatchError("Int", "com.softwaremill.domain.SomeType"),
      TypeMismatchError("String", "prefix.com.softwaremill.domain.SomeType"),
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