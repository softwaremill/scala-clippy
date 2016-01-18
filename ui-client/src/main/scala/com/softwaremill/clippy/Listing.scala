package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import autowire._
import scala.concurrent.ExecutionContext.Implicits.global

object Listing {
  case class State(advices: Seq[AdviceListing])

  class Backend($: BackendScope[Unit, State]) {
    def render(s: State) = <.table(^.cls := "table table-striped")(
      <.thead(
        <.tr(
          <.th("Compilation error"),
          <.th("Advice"),
          <.th("Library"),
          <.th("Suggest edit")
        )
      ),
      <.tbody(
        s.advices.map(a =>
          <.tr(
            <.td(a.compilationError.toString),
            <.td(a.advice),
            <.td(a.library.toString),
            <.td(<.span(^.cls := "glyphicon glyphicon-edit"))
          )): _*
      )
    )

    def fetch(): Callback = {
      CallbackTo(AutowireClient[UiApi].listAccepted().call()).map(
        _.map(s => $.setState(State(s)).runNow())
      )
    }
  }

  val component = ReactComponentB[Unit]("Use")
    .initialState(State(Nil))
    .renderBackend[Backend]
    .componentDidMount(_.backend.fetch())
    .buildU
}
