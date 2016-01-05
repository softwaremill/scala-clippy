package com.softwaremill.clippy

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.{FlatSpec, Matchers}

class NotFoundAdviceTest extends FlatSpec with Matchers {

  behavior of "NotFound Advice"

  it should "match exact expression" in {
    // given
    val advice = NotFoundAdvice("value wire[]", "Perhaps you should switch to PHP?")
    val matchingErr = NotFoundError("value wire[]")
    val nonMatchingErrs = List(
      NotFoundError("value wirex"),
      NotFoundError("value wir"),
      NotFoundError("avalue wire"),
      NotFoundError("hakuna matata")
    )

    // then
    advice.errMatching should be definedAt matchingErr
    nonMatchingErrs.foreach(advice.errMatching should not be definedAt(_))
  }

  it should "match regex" in {
    // given
    val advice = NotFoundAdvice("value wi.*[0-9]*", "Please double-check your version of Spring framework.")
    val matchingErrs = List(
      NotFoundError("value wire"),
      NotFoundError("value wirex"),
      NotFoundError("value wi"),
      NotFoundError("value wire55")
    )
    val nonMatchingErrs = List(
      NotFoundError("avalue wire"),
      NotFoundError("avalue w5")
    )

    // then
    matchingErrs.foreach(advice.errMatching should be definedAt (_))
    nonMatchingErrs.foreach(advice.errMatching should not be definedAt(_))
  }

}

class NotFoundAdviceProperties extends Properties("NotFound advice") {
  property("matches identical string") = forAll { (what: String) =>
    NotFoundAdvice(what, "Add some more RAM").errMatching.isDefinedAt(NotFoundError(what))
  }
}