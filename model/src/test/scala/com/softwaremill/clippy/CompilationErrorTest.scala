package com.softwaremill.clippy

import org.scalacheck.Prop._
import org.scalacheck.Properties
import org.scalatest.{Matchers, FlatSpec}

class CompilationErrorTest extends FlatSpec with Matchers {
  it should "do not match different exact not found errors" in {
    // given
    val err = NotFoundError(RegexT.fromPattern("value wire[]"))
    val nonMatchingErrs = List(
      NotFoundError(ExactT("value wirex")),
      NotFoundError(ExactT("value wir")),
      NotFoundError(ExactT("avalue wire")),
      NotFoundError(ExactT("hakuna matata"))
    )

    // then
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }

  it should "match regex in not found errors" in {
    // given
    val err = NotFoundError(RegexT.fromPattern("value wi*"))
    val matchingErrs = List(
      NotFoundError(ExactT("value wire")),
      NotFoundError(ExactT("value wirex")),
      NotFoundError(ExactT("value wi")),
      NotFoundError(ExactT("value wire55"))
    )
    val nonMatchingErrs = List(
      NotFoundError(ExactT("avalue wire")),
      NotFoundError(ExactT("avalue w5"))
    )

    // then
    matchingErrs.foreach(err.matches(_) should be (true))
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }

  it should "not match different exact type mismatch errors" in {
    // given
    val err = TypeMismatchError(RegexT.fromPattern("com.softwaremill.String"), None,
      RegexT.fromPattern("com.softwaremill.RequiredType[String]"), None)

    val nonMatchingErrs = List(
      new TypeMismatchError(ExactT("com.softwaremill.String"), None, ExactT("com.softwaremill.OtherType"), None),
      new TypeMismatchError(ExactT("com.softwaremill.Int"), None, ExactT("com.softwaremill.OtherType"), None),
      new TypeMismatchError(ExactT("com.softwaremill.Int"), None, ExactT("com.softwaremill.RequiredType[String]"), None)
    )

    // then
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }

  it should "match regex in type mismatch errors" in {
    // given
    val err = new TypeMismatchError(RegexT.fromPattern("slick.dbio.DBIOAction[*]"), None,
      RegexT.fromPattern("slick.lifted.Rep[Option[*]]"), None)
    val matchingErrs = List(
      TypeMismatchError(ExactT("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]"), None,
        ExactT("slick.lifted.Rep[Option[?]]"), None),
      TypeMismatchError(ExactT("slick.dbio.DBIOAction[String,slick.dbio.NoStream,slick.dbio.Effect.Read]"), None,
        ExactT("slick.lifted.Rep[Option[Int]]"), None),
      TypeMismatchError(ExactT("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]"), None,
        ExactT("slick.lifted.Rep[Option[Option[Int]]"), None)
    )
    val nonMatchingErrs = List(
      TypeMismatchError(ExactT("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]"), None, ExactT("String"), None),
      TypeMismatchError(ExactT("String"), None, ExactT("slick.lifted.Rep[Option[?]]"), None),
      TypeMismatchError(ExactT("String"), None, ExactT("com.softwaremill.AweSomeType"), None)
    )

    // then
    matchingErrs.foreach(err.matches(_) should be (true))
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }
}

class CompilationErrorProperties extends Properties("CompilationError") {
  property("obj -> json -> obj works for type mismatch error") =
    forAll { (found: String, foundExpandsTo: Option[String], required: String, requiredExpandsTo: Option[String]) =>
      val e = TypeMismatchError(RegexT.fromPattern(found), foundExpandsTo.map(RegexT.fromPattern),
        RegexT.fromPattern(required), requiredExpandsTo.map(RegexT.fromPattern))
      CompilationError.fromJson(e.toJson).contains(e)
    }

  property("obj -> json -> obj works for not found error") =
    forAll { (what: String) =>
      val e = NotFoundError(RegexT.fromPattern(what))
      CompilationError.fromJson(e.toJson).contains(e)
    }

  property("obj -> json -> obj works for not a member error") =
    forAll { (what: String, notAMemberOf: String) =>
      val e = NotAMemberError(RegexT.fromPattern(what), RegexT.fromPattern(notAMemberOf))
      CompilationError.fromJson(e.toJson).contains(e)
    }

  property("obj -> json -> obj works for implicit not found") =
    forAll { (parameter: String, implicitType: String) =>
      val e = ImplicitNotFoundError(RegexT.fromPattern(parameter), RegexT.fromPattern(implicitType))
      CompilationError.fromJson(e.toJson).contains(e)
    }

  property("obj -> json -> obj works for diverging implicit expansions") =
    forAll { (forType: String, startingWith: String, in: String) =>
      val e = DivergingImplicitExpansionError(RegexT.fromPattern(forType), RegexT.fromPattern(startingWith),
        RegexT.fromPattern(in))
      CompilationError.fromJson(e.toJson).contains(e)
    }

  property("match identical not found error") =
    forAll { (what: String) =>
      NotFoundError(RegexT.fromPattern(what)).matches(NotFoundError(ExactT(what)))
    }

  property("matches identical type mismatch error") = forAll { (found: String, required: String) =>
    TypeMismatchError(RegexT.fromPattern(found), None, RegexT.fromPattern(required), None).matches(
      TypeMismatchError(ExactT(found), None, ExactT(required), None))
  }
}
