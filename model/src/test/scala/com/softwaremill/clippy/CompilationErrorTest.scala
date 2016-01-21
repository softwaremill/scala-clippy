package com.softwaremill.clippy

import org.scalacheck.Prop._
import org.scalacheck.Properties
import org.scalatest.{Matchers, FlatSpec}

class CompilationErrorTest extends FlatSpec with Matchers {
  it should "do not match different exact not found errors" in {
    // given
    val err = NotFoundError(ExactOrRegex("value wire[]"))
    val nonMatchingErrs = List(
      NotFoundError(Exact("value wirex")),
      NotFoundError(Exact("value wir")),
      NotFoundError(Exact("avalue wire")),
      NotFoundError(Exact("hakuna matata"))
    )

    // then
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }

  it should "match regex in not found errors" in {
    // given
    val err = NotFoundError(ExactOrRegex("value wi.*[0-9]*"))
    val matchingErrs = List(
      NotFoundError(Exact("value wire")),
      NotFoundError(Exact("value wirex")),
      NotFoundError(Exact("value wi")),
      NotFoundError(Exact("value wire55"))
    )
    val nonMatchingErrs = List(
      NotFoundError(Exact("avalue wire")),
      NotFoundError(Exact("avalue w5"))
    )

    // then
    matchingErrs.foreach(err.matches(_) should be (true))
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }

  it should "do not match different exact type mismatch errors" in {
    // given
    val err = TypeMismatchError(ExactOrRegex("com.softwaremill.String"), None,
      ExactOrRegex("com.softwaremill.RequiredType[String]"), None)

    val nonMatchingErrs = List(
      new TypeMismatchError(Exact("com.softwaremill.String"), None, Exact("com.softwaremill.OtherType"), None),
      new TypeMismatchError(Exact("com.softwaremill.Int"), None, Exact("com.softwaremill.OtherType"), None),
      new TypeMismatchError(Exact("com.softwaremill.Int"), None, Exact("com.softwaremill.RequiredType[String]"), None)
    )

    // then
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }

  it should "match regex in type mismatch errors" in {
    // given
    val err = new TypeMismatchError(ExactOrRegex("slick.dbio.DBIOAction\\[.*\\]"), None,
      ExactOrRegex("slick.lifted.Rep\\[Option\\[.*\\]\\]"), None)
    val matchingErrs = List(
      TypeMismatchError(Exact("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Write]"), None,
        Exact("slick.lifted.Rep[Option[?]]"), None),
      TypeMismatchError(Exact("slick.dbio.DBIOAction[String,slick.dbio.NoStream,slick.dbio.Effect.Read]"), None,
        Exact("slick.lifted.Rep[Option[Int]]"), None),
      TypeMismatchError(Exact("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]"), None,
        Exact("slick.lifted.Rep[Option[Option[Int]]"), None)
    )
    val nonMatchingErrs = List(
      TypeMismatchError(Exact("slick.dbio.DBIOAction[Unit,slick.dbio.NoStream,slick.dbio.Effect.Read]"), None, Exact("String"), None),
      TypeMismatchError(Exact("String"), None, Exact("slick.lifted.Rep[Option[?]]"), None),
      TypeMismatchError(Exact("String"), None, Exact("com.softwaremill.AweSomeType"), None)
    )

    // then
    matchingErrs.foreach(err.matches(_) should be (true))
    nonMatchingErrs.foreach(err.matches(_) should be (false))
  }
}

class CompilationErrorProperties extends Properties("CompilationError") {
  property("obj -> xml -> obj works for type mismatch error") =
    forAll { (found: String, foundExpandsTo: Option[String], required: String, requiredExpandsTo: Option[String]) =>
      val e = TypeMismatchError(ExactOrRegex(found), foundExpandsTo.map(ExactOrRegex),
        ExactOrRegex(required), requiredExpandsTo.map(ExactOrRegex))
      CompilationError.fromXml(e.toXml).contains(e)
    }

  property("obj -> xml -> obj works for not found error") =
    forAll { (what: String) =>
      val e = NotFoundError(ExactOrRegex(what))
      CompilationError.fromXml(e.toXml).contains(e)
    }

  property("obj -> xml -> obj works for not a member error") =
    forAll { (what: String, notAMemberOf: String) =>
      val e = NotAMemberError(ExactOrRegex(what), ExactOrRegex(notAMemberOf))
      CompilationError.fromXml(e.toXml).contains(e)
    }

  property("obj -> xml -> obj works for implicit not found") =
    forAll { (parameter: String, implicitType: String) =>
      val e = ImplicitNotFound(ExactOrRegex(parameter), ExactOrRegex(implicitType))
      CompilationError.fromXml(e.toXml).contains(e)
    }

  property("match identical not found error") =
    forAll { (what: String) =>
      NotFoundError(ExactOrRegex(what)).matches(NotFoundError(Exact(what)))
    }

  property("matches identical type mismatch error") = forAll { (found: String, required: String) =>
    TypeMismatchError(ExactOrRegex(found), None, ExactOrRegex(required), None).matches(
      TypeMismatchError(Exact(found), None, Exact(required), None))
  }
}
