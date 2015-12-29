package org.softwaremill.clippy

import org.scalatest.{FlatSpec, Matchers}

class TypeMismatchAdviceTest extends FlatSpec with Matchers {

  behavior of "Type Mismatch Advice"

  it should "match exact expression" in {
    // given
    val advice = new TypeMismatchAdvice("found", "required", "adviceText")
    val matchingErr = TypeMismatchError("found", "required")
    val nonMatchingErrs = List(
      new TypeMismatchError("found", "not matching required"),
      new TypeMismatchError("not matching found", "not matching required"),
      new TypeMismatchError("not matching found", "required")
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
