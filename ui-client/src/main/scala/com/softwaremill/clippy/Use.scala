package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Use {
  val component = ReactComponentB[Unit]("Use")
    .render { $ =>
      <.p("Here's how to use Clippy and what it does. Add the plugin and that's it!")
    }
    .buildU
}
