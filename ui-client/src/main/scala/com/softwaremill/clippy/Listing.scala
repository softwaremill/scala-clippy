package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import autowire._
import scala.concurrent.ExecutionContext.Implicits.global

object Listing {
  case class State(advices: Seq[AdviceListing])

  class Backend($: BackendScope[Unit, State]) {
    def render(s: State) = <.div(
      s"Listing ${s.advices.size}"
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
