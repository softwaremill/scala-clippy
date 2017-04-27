package com.softwaremill.clippy

import autowire._
import com.softwaremill.clippy.BsUtils._
import com.softwaremill.clippy.Utils._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses
import scala.concurrent.ExecutionContext.Implicits.global

object Feedback {
  case class Props(showError: String => Callback, clearMsgs: Callback, handleFuture: HandleFuture)

  @Lenses
  case class State(contact: FormField, feedback: FormField)
  implicit val stateVal = new Validatable[State] {
    override def validated(s: State) = s.copy(
      contact = s.contact.validated,
      feedback = s.feedback.validated
    )
    override def fields(s: State) = List(s.contact, s.feedback)
  }

  class Backend($ : BackendScope[Props, State]) {
    def render(s: State, p: Props) = {
      def sendFeedbackCallback() =
        p.handleFuture(
          AutowireClient[UiApi].feedback(s.feedback.v, s.contact.v).call(),
          Some("Feedback sent, thank you!"),
          Some(
            (_: Unit) => $.modState(s => s.copy(contact = s.contact.copy(v = ""), feedback = s.feedback.copy(v = "")))
          )
        )

      <.form(
        ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => sendFeedbackCallback()),
        bsFormEl(externalVar($, s, State.contact))(
          mods =>
            <.input(^.`type` := "email", ^.cls := "form-control", ^.placeholder := "scalacoder@company.com")(mods)
        ),
        bsFormEl(externalVar($, s, State.feedback))(mods => <.textarea(^.cls := "form-control", ^.rows := "3")(mods)),
        <.button(^.`type` := "send", ^.cls := "btn btn-primary")("Send")
      )
    }
  }

  val component = ReactComponentB[Props]("Use")
    .initialState(
      State(
        FormField("Contact email", required = true),
        FormField("Feedback", required = true)
      )
    )
    .renderBackend[Backend]
    .build
}
