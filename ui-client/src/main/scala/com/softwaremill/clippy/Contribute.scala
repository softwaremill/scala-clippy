package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import BsUtil._

object Contribute {
  object Step1 {
    case class Props(submit: String => Callback, showError: String => Callback)

    case class State(errorText: FormField)
    implicit val stateVal = new Validatable[State] {
      override def validated(s: State) = s.copy(errorText = s.errorText.validated)
      override def fields(s: State) = List(s.errorText)
    }

    class Backend($: BackendScope[Props, State]) {
      def render(s: State, p: Props) = <.div(
        bsPanel(
          <.p("Scala Clippy is only as good as its advice database. Help other users by submitting a fix for a compilation error that you have encountered!"),
          <.p("First, paste in the error and we'll see if we can parse it. Only the error message is needed, without the file name and code snippet, e.g.:"),
          <.pre(
            """type mismatch;
              |found   : akka.http.scaladsl.server.StandardRoute
              |required: akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]""".stripMargin
          )
        ),
        <.form(
          ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => p.submit(s.errorText.v)),
          bsFormEl(s.errorText, net => $.modState(s => s.copy(errorText = net)))(
            <.textarea(^.cls := "form-control", ^.rows := 3)
          ),
          <.button(^.`type` := "submit", ^.cls := "btn btn-primary")("Next")
        )
      )
    }

    val component = ReactComponentB[Props]("ContributeStep1")
      .initialState(State(FormField.initial("Error text", required = true)))
      .renderBackend[Backend]
      .build
  }

  object Step2 {
    val component = ReactComponentB[Unit]("ContributeStep2")
      .render { $ => <.div("Parsed!") }
      .buildU
  }

  object ParseError {
    case class Props(reset: Callback, send: String => Callback, showError: String => Callback)

    case class State(email: FormField)
    implicit val stateVal = new Validatable[State] {
      override def validated(s: State) = s.copy(email = s.email.validated)
      override def fields(s: State) = List(s.email)
    }

    class Backend($: BackendScope[Props, State]) {
      def render(s: State, p: Props) = <.div(
        <.div(
          bsPanel(
            <.p("Unfortunately we cannot parse the error. Let us know how to contact you, we'll try to find out what's wrong and get back to you.")
          ),
          <.form(
            ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => p.send(s.email.v)),
            bsFormEl(s.email, ne => $.modState(s => s.copy(email = ne)))(
              <.input(^.`type` := "email", ^.cls := "form-control", ^.placeholder := "scalacoder@company.com")
            ),
            <.button(^.`type` := "reset", ^.cls := "btn btn-default", ^.onClick --> p.reset)("Reset"),
            <.button(^.`type` := "submit", ^.cls := "btn btn-primary")("Send")
          )
        )
      )
    }

    val component = ReactComponentB[Props]("ContributeParseError")
      .initialState(State(FormField.initial("Email", required = true)))
      .renderBackend[Backend]
      .build
  }
}
