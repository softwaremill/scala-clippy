package com.softwaremill.clippy

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class StringDiffSpecification extends Properties("StringDiff") with TypeNamesGenerators {

  val S     = "S"
  val E     = "E"
  val AddSE = (s: String) => S + s + E

  def innerTypeDiffsCorrectly(fourTypes: List[String]): Boolean = {
    val List(x, y, v, z) = fourTypes
    val expected         = s"$x[$y[$z]]"
    val actual           = s"$x[$v[$z]]"
    val msg              = new StringDiff(expected, actual, AddSE).diff("expected: %s actual: %s")
    msg == s"""expected: $x[$S$y$E[$z]] actual: $x[$S$v$E[$z]]"""
  }

  def twoTypesAreFullyDiff(twoTypes: List[String]): Boolean = {
    val List(x, y) = twoTypes
    new StringDiff(x, y, AddSE).diff("expected: %s actual: %s") == s"""expected: $S$x$E actual: $S$y$E"""
  }

  property("X[Y[Z]] vs X[V[Z]] always gives X[<diff>[Z]] excluding packages") =
    forAll(different(singleTypeName)(4))(innerTypeDiffsCorrectly)

  property("X[Y[Z]] vs X[V[Z]] always gives X[<diff>[Z]] if Y and V have common prefix") =
    forAll(typesWithCommonPrefix(4))(innerTypeDiffsCorrectly)

  property("X[Y[Z]] vs X[V[Z]] always gives X[<diff>[Z]] if Y and V have common suffix") =
    forAll(typesWithCommonSuffix(4))(innerTypeDiffsCorrectly)

  property("A[X] vs B[X] always marks outer as diff for A != B when A and B have common prefix") =
    forAll(typesWithCommonPrefix(2), complexTypeName(maxDepth = 3)) { (outerTypes, x) =>
      val List(a, b) = outerTypes
      val expected   = s"$a[$x]"
      val actual     = s"$b[$x]"
      val msg        = new StringDiff(expected, actual, AddSE).diff("expected: %s actual: %s")
      msg == s"""expected: $S$a$E[$x] actual: $S$b$E[$x]"""
    }

  property("package.A[X] vs package.B[X] always gives package.<diff>A</diff>[X]") =
    forAll(javaPackage, different(singleTypeName)(2), complexTypeName(maxDepth = 3)) { (pkg, outerTypes, x) =>
      val List(a, b) = outerTypes
      val expected   = s"$pkg$a[$x]"
      val actual     = s"$pkg$b[$x]"
      val msg        = new StringDiff(expected, actual, AddSE).diff("expected: %s actual: %s")
      msg == s"""expected: $pkg$S$a$E[$x] actual: $pkg$S$b$E[$x]"""
    }

  property("any complex X vs Y is a full diff when X and Y don't have common suffix nor prefix") =
    forAll(different(complexTypeName(maxDepth = 4))(2).suchThat(noCommonPrefixSuffix))(twoTypesAreFullyDiff)

  property("any single X vs Y is a full diff") = forAll(different(singleTypeName)(2))(twoTypesAreFullyDiff)

  def noCommonPrefixSuffix(twoTypes: List[String]): Boolean = {
    val List(x, y) = twoTypes
    x.head != y.head && x.last != y.last
  }

}
