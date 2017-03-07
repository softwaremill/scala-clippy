package com.softwaremill.clippy

import org.scalatest.{FlatSpec, Matchers}

class CompilationErrorParserTest extends FlatSpec with Matchers {
  it should "parse akka's route error message" in {
    val e =
      """type mismatch;
        | found   : akka.http.scaladsl.server.StandardRoute
        | required: akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]""".stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      ExactT("akka.http.scaladsl.server.StandardRoute"),
      None,
      ExactT("akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]"),
      None,
      None
    )))
  }

  it should "parse an error message with [error] prefix" in {
    val e =
      """[error] /Users/adamw/projects/clippy/tests/src/main/scala/com/softwaremill/clippy/Working.scala:16: type mismatch;
        |[error]  found   : akka.http.scaladsl.server.StandardRoute
        |[error]  required: akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]""".stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      ExactT("akka.http.scaladsl.server.StandardRoute"),
      None,
      ExactT("akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]"),
      None,
      None
    )))
  }

  it should "parse a type mismatch error with a single expands to section" in {
    val e =
      """type mismatch;
        |found   : japgolly.scalajs.react.CompState.ReadCallbackWriteCallbackOps[com.softwaremill.clippy.Contribute.Step2.State]#This[com.softwaremill.clippy.FormField]
        |required: japgolly.scalajs.react.CompState.AccessRD[?]
        |   (which expands to)  japgolly.scalajs.react.CompState.ReadDirectWriteCallbackOps[?]""".stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      ExactT("japgolly.scalajs.react.CompState.ReadCallbackWriteCallbackOps[com.softwaremill.clippy.Contribute.Step2.State]#This[com.softwaremill.clippy.FormField]"),
      None,
      ExactT("japgolly.scalajs.react.CompState.AccessRD[?]"),
      Some(ExactT("japgolly.scalajs.react.CompState.ReadDirectWriteCallbackOps[?]")),
      None
    )))
  }

  it should "parse a type mismatch error with two expands to sections" in {
    val e =
      """type mismatch;
        |found   : japgolly.scalajs.react.CompState.ReadCallbackWriteCallbackOps[com.softwaremill.clippy.Contribute.Step2.State]#This[com.softwaremill.clippy.FormField]
        |   (which expands to)  japgolly.scalajs.react.CompState.ReadCallbackWriteCallbackOps[com.softwaremill.clippy.FormField]
        |required: japgolly.scalajs.react.CompState.AccessRD[?]
        |   (which expands to)  japgolly.scalajs.react.CompState.ReadDirectWriteCallbackOps[?]""".stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      ExactT("japgolly.scalajs.react.CompState.ReadCallbackWriteCallbackOps[com.softwaremill.clippy.Contribute.Step2.State]#This[com.softwaremill.clippy.FormField]"),
      Some(ExactT("japgolly.scalajs.react.CompState.ReadCallbackWriteCallbackOps[com.softwaremill.clippy.FormField]")),
      ExactT("japgolly.scalajs.react.CompState.AccessRD[?]"),
      Some(ExactT("japgolly.scalajs.react.CompState.ReadDirectWriteCallbackOps[?]")),
      None
    )))
  }

  it should "parse macwire's wire not found error message" in {
    val e = "not found: value wire"

    CompilationErrorParser.parse(e) should be (Some(NotFoundError(ExactT("value wire"))))
  }

  it should "parse not a member of message" in {
    val e = "value call is not a member of scala.concurrent.Future[Unit]"

    CompilationErrorParser.parse(e) should be (Some(NotAMemberError(ExactT("value call"), ExactT("scala.concurrent.Future[Unit]"))))
  }

  it should "parse not a member of message with extra text" in {
    val e = "[error] /Users/adamw/projects/clippy/ui-client/src/main/scala/com/softwaremill/clippy/Listing.scala:33: value call is not a member of scala.concurrent.Future[Unit]"

    CompilationErrorParser.parse(e) should be (Some(NotAMemberError(ExactT("value call"), ExactT("scala.concurrent.Future[Unit]"))))
  }

  it should "parse an implicit not found" in {
    val e = "could not find implicit value for parameter marshaller: spray.httpx.marshalling.ToResponseMarshaller[scala.concurrent.Future[String]]"

    CompilationErrorParser.parse(e) should be (Some(ImplicitNotFoundError(ExactT("marshaller"), ExactT("spray.httpx.marshalling.ToResponseMarshaller[scala.concurrent.Future[String]]"))))
  }

  it should "parse a diverging implicit error " in {
    val e = "diverging implicit expansion for type io.circe.Decoder.Secondary[this.Out] starting with method decodeCaseClass in trait GenericInstances"

    CompilationErrorParser.parse(e) should be (Some(DivergingImplicitExpansionError(
      ExactT("io.circe.Decoder.Secondary[this.Out]"), ExactT("decodeCaseClass"), ExactT("trait GenericInstances"))))
  }

  it should "parse a diverging implicit error with extra text" in {
    val e =
      """
        |[error] /home/src/main/scala/Routes.scala:19: diverging implicit expansion for type io.circe.Decoder.Secondary[this.Out]
        |[error] starting with method decodeCaseClass in trait GenericInstances
      """.stripMargin

    CompilationErrorParser.parse(e) should be (Some(DivergingImplicitExpansionError(
      ExactT("io.circe.Decoder.Secondary[this.Out]"), ExactT("decodeCaseClass"), ExactT("trait GenericInstances"))))
  }

  it should "parse a type arguments do not conform to any overloaded bounds error" in {
    val e =
      """
        |[error]  clippy/Working.scala:32: type arguments [org.softwaremill.clippy.User] conform to the bounds of none of the overloaded alternatives of
        |value apply: [E <: slick.lifted.AbstractTable[_]]=> slick.lifted.TableQuery[E] <and> [E <: slick.lifted.AbstractTable[_]](cons: slick.lifted.Tag => E)slick.lifted.TableQuery[E]
        |protected val users = TableQuery[User]
      """.stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeArgumentsDoNotConformToOverloadedBoundsError(
      ExactT("org.softwaremill.clippy.User"), ExactT("value apply"), Set(
        ExactT("[E <: slick.lifted.AbstractTable[_]]=> slick.lifted.TableQuery[E]"),
        ExactT("[E <: slick.lifted.AbstractTable[_]](cons: slick.lifted.Tag => E)slick.lifted.TableQuery[E]")))))
  }

  it should "parse a no implicit defined for" in {
    val e =
      """
        |[error] /Users/clippy/model/src/main/scala/com/softwaremill/clippy/CompilationErrorParser.scala:18: No implicit Ordering defined for java.time.LocalDate.
        |[error]   Seq(java.time.LocalDate.MIN, java.time.LocalDate.MAX).sorted
      """.stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeclassNotFoundError(
      ExactT("Ordering"), ExactT("java.time.LocalDate"))))
  }

  it should "parse an error with notes" in {
    val e =
      """
        |type mismatch;
        | found   : org.softwaremill.clippy.ImplicitResolutionDiamond.C
        | required: Array[String]
        |Note that implicit conversions are not applicable because they are ambiguous:
        | both method toMessage in object B of type (b: org.softwaremill.clippy.ImplicitResolutionDiamond.B)Array[String]
        | and method toMessage in object A of type (a: org.softwaremill.clippy.ImplicitResolutionDiamond.A)Array[String]
        | are possible conversion functions from org.softwaremill.clippy.ImplicitResolutionDiamond.C to Array[String]
      """.stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      ExactT("org.softwaremill.clippy.ImplicitResolutionDiamond.C"),
      None,
      ExactT("Array[String]"),
      None,
      Some("""Note that implicit conversions are not applicable because they are ambiguous:
             | both method toMessage in object B of type (b: org.softwaremill.clippy.ImplicitResolutionDiamond.B)Array[String]
             | and method toMessage in object A of type (a: org.softwaremill.clippy.ImplicitResolutionDiamond.A)Array[String]
             | are possible conversion functions from org.softwaremill.clippy.ImplicitResolutionDiamond.C to Array[String]""".stripMargin)
    )))
  }

  it should "parse an error with expands to & notes" in {
    val e =
      """
        |type mismatch;
        | found   : org.softwaremill.clippy.ImplicitResolutionDiamond.C
        | required: Array[String]
        |   (which expands to)  japgolly.scalajs.react.CompState.ReadDirectWriteCallbackOps[?]
        |Note that implicit conversions are not applicable because they are ambiguous:
        | both method toMessage in object B of type (b: org.softwaremill.clippy.ImplicitResolutionDiamond.B)Array[String]
        | and method toMessage in object A of type (a: org.softwaremill.clippy.ImplicitResolutionDiamond.A)Array[String]
        | are possible conversion functions from org.softwaremill.clippy.ImplicitResolutionDiamond.C to Array[String]
      """.stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      ExactT("org.softwaremill.clippy.ImplicitResolutionDiamond.C"),
      None,
      ExactT("Array[String]"),
      Some(ExactT("japgolly.scalajs.react.CompState.ReadDirectWriteCallbackOps[?]")),
      Some("""Note that implicit conversions are not applicable because they are ambiguous:
             | both method toMessage in object B of type (b: org.softwaremill.clippy.ImplicitResolutionDiamond.B)Array[String]
             | and method toMessage in object A of type (a: org.softwaremill.clippy.ImplicitResolutionDiamond.A)Array[String]
             | are possible conversion functions from org.softwaremill.clippy.ImplicitResolutionDiamond.C to Array[String]""".stripMargin)
    )))
  }
}
