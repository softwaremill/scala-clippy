package com.softwaremill.clippy

import japgolly.scalajs.react._
import org.scalajs.jquery._
import scala.scalajs.js
import japgolly.scalajs.react.vdom.prefix_<^._

object Main extends js.JSApp {
  type HtmlId = String

  def main(): Unit = {
    jQuery(setupUI _)
  }

  def setupUI(): Unit = {
    val mountNode = org.scalajs.dom.document.getElementById("reactmain")

    val app = ReactComponentB[Unit]("App")
      .initialState(App.State(App.UsePage, Nil, Nil))
      .renderBackend[App.Backend]
      .buildU

    ReactDOM.render(app(), mountNode)
  }
}