package com.softwaremill.clippy

import org.scalacheck.Gen

trait TypeNamesGenerators {

  import Gen._

  def typeNameChar = frequency((1, numChar), (3, Gen.const('_')), (14, alphaChar))

  val specialTypeName = Gen.oneOf(List("Option", "Operation", "Op", "String", "Long", "Set", "Aux"))

  def javaPackage: Gen[String] =
    for {
      depth <- Gen.choose(0, 3)
      names <- Gen.listOfN(depth, packageIdentifer)
    } yield {
      val str = names.filter(_.nonEmpty).mkString(".")
      if (str.nonEmpty)
        str + "."
      else
        str
    }

  def packageIdentifer: Gen[String] =
    for {
      c  <- alphaLowerChar
      cs <- Gen.resize(7, listOf(alphaNumChar))
    } yield {
      (c :: cs).mkString
    }

  def randomTypeWithPackage: Gen[String] =
    for {
      p <- javaPackage
      t <- randomTypeWithPackage
    } yield p + t

  def randomTypeName: Gen[String] =
    for {
      c  <- alphaChar
      cs <- Gen.resize(7, listOf(typeNameChar))
    } yield (c :: cs).mkString

  def singleTypeName = frequency((3, randomTypeName), (7, specialTypeName))

  def functionalTypeName(maxResultDepth: Int): Gen[String] =
    for {
      argsMemberCount <- Gen.choose(2, 4)
      args            <- Gen.oneOf(singleTypeName, tupleTypeName(depth = 0, argsMemberCount))
      result          <- complexTypeName(maxResultDepth)
    } yield {
      s"$args => $result"
    }

  def genericTypeName(depth: Int, memberCount: Int): Gen[String] =
    for {
      name       <- singleTypeName
      innerNames <- Gen.listOfN(memberCount, if (depth == 0) singleTypeName else complexTypeName(depth - 1))
    } yield s"$name[${innerNames.mkString(", ")}]"

  def tupleTypeName(depth: Int, memberCount: Int): Gen[String] =
    for {
      innerNames <- Gen.listOfN(memberCount, if (depth == 0) singleTypeName else complexTypeName(depth - 1))
    } yield s"(${innerNames.mkString(", ")})"

  def different[T](gen: Gen[T]) =
    (count: Int) =>
      Gen.listOfN(count, gen).suchThat { list =>
        list.distinct == list
    }

  def typesWithCommonPrefix =
    (count: Int) =>
      for {
        types        <- different(singleTypeName)(count)
        commonPrefix <- singleTypeName
      } yield types.map(commonPrefix + _)

  def typesWithCommonSuffix =
    (count: Int) =>
      for {
        types        <- different(singleTypeName)(count)
        commonSuffix <- singleTypeName
      } yield types.map(_ + commonSuffix)

  def flatTypeIdentifier =
    for {
      tupleMemberCount <- Gen.choose(2, 4)
      generator <- Gen.oneOf(
        Seq(singleTypeName, tupleTypeName(0, tupleMemberCount), innerTypeName(0), functionalTypeName(0))
      )
      typeStr <- generator
    } yield typeStr

  def deepTypeName(maxDepth: Int): Gen[String] =
    for {
      genericMemberCount <- Gen.choose(1, 3)
      tupleMemberCount   <- Gen.choose(2, 4)
      generator <- Gen.oneOf(
        Seq(
          singleTypeName,
          tupleTypeName(maxDepth, tupleMemberCount),
          genericTypeName(maxDepth, genericMemberCount),
          innerTypeName(maxDepth),
          functionalTypeName(maxDepth)
        )
      )
      typeStr <- generator
    } yield typeStr

  def complexTypeName(maxDepth: Int): Gen[String] =
    if (maxDepth == 0)
      flatTypeIdentifier
    else
      deepTypeName(maxDepth)

  def innerTypeName(maxDepth: Int): Gen[String] =
    for {
      outerName <- singleTypeName
      innerType <- complexTypeName(maxDepth)
    } yield s"$outerName#$innerType"
}
