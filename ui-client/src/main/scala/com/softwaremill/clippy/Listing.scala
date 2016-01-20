package com.softwaremill.clippy

import com.softwaremill.clippy.BsUtils._
import com.softwaremill.clippy.Utils._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import autowire._
import monocle.macros.Lenses
import scala.concurrent.ExecutionContext.Implicits.global

object Listing {
  case class Props(showError: String => Callback, showInfo: String => Callback, clearMsgs: Callback)

  @Lenses
  case class State(advices: Seq[AdviceListing], suggestEditId: Option[Long],
    suggestContact: FormField, suggestText: FormField)
  implicit val stateVal = new Validatable[State] {
    override def validated(s: State) = s.copy(
      suggestContact = s.suggestContact.validated,
      suggestText = s.suggestText.validated
    )
    override def fields(s: State) = List(s.suggestContact, s.suggestText)
  }

  class Backend($: BackendScope[Props, State]) {
    def render(s: State, p: Props) = {
      def suggestEditCallback(a: AdviceListing) = p.clearMsgs >> $.modState(s =>
        s.copy(suggestEditId = Some(a.id), suggestText = s.suggestText.copy(v = "")))
      def cancelSuggestEditCallback(a: AdviceListing) = $.modState(_.copy(suggestEditId = None))
      def sendSuggestEditCallback(a: AdviceListing) =
        CallbackTo(AutowireClient[UiApi].sendSuggestEdit(s.suggestText.v, s.suggestContact.v, a).call()).flatMap(_ =>
          p.showInfo("Suggestion sent, thank you!") >> cancelSuggestEditCallback(a))

      def rowForAdvice(a: AdviceListing) = <.tr(
        <.td(a.compilationError.toString),
        <.td(a.advice),
        <.td(a.library.toString),
        <.td(<.span(^.cls := "glyphicon glyphicon-edit", ^.onClick --> suggestEditCallback(a)))
      )

      def suggestEdit(a: AdviceListing) = <.tr(
        <.td(^.colSpan := 4)(
          <.form(
            ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => sendSuggestEditCallback(a)),
            bsFormEl(externalVar($, s, State.suggestContact))(mods =>
              <.input(^.`type` := "email", ^.cls := "form-control", ^.placeholder := "scalacoder@company.com")(mods)),
            bsFormEl(externalVar($, s, State.suggestText))(mods =>
              <.textarea(^.cls := "form-control", ^.rows := "3")(mods)),
            <.button(^.`type` := "reset", ^.cls := "btn btn-default", ^.onClick --> cancelSuggestEditCallback(a))("Cancel"),
            <.span(" "),
            <.button(^.`type` := "send", ^.cls := "btn btn-primary")("Send")
          )
        )
      )

      <.table(^.cls := "table table-striped")(
        <.thead(
          <.tr(
            <.th("Compilation error"),
            <.th("Advice"),
            <.th("Library"),
            <.th("Suggest edit")
          )
        ),
        <.tbody(
          s.advices.flatMap(a =>
            List(rowForAdvice(a)) ++ s.suggestEditId.filter(_ == a.id).map(_ => suggestEdit(a)).toList): _*
        )
      )
    }

    def initAdvices(): Callback = {
      CallbackTo(AutowireClient[UiApi].listAccepted().call()).map(
        _.map(s => $.modState(_.copy(advices = s)).runNow())
      )
    }
  }

  val component = ReactComponentB[Props]("Use")
    .initialState(State(Nil, None, FormField("Contact email (optional)", required = false),
      FormField("Suggestion", required = true)))
    .renderBackend[Backend]
    .componentDidMount(_.backend.initAdvices())
    .build
}
