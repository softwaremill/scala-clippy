package org.softwaremill.clippy

import org.scalatest.{FlatSpec, Matchers}

class CompilationErrorParserTest extends FlatSpec with Matchers {
  it should "parse akka's route error message" in {
    val e =
      """type mismatch;
        | found   : akka.http.scaladsl.server.StandardRoute
        | required: akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]""".stripMargin

    CompilationErrorParser.parse(e) should be (Some(TypeMismatchError(
      "akka.http.scaladsl.server.StandardRoute",
      "akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]"
    )))
  }

  it should "parse macwire's wire not found error message" in {
    val e = "not found: value wire"

    CompilationErrorParser.parse(e) should be (Some(NotFoundError("value wire")))
  }
}
