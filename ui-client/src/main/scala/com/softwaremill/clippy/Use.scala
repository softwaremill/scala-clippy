package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Use {
  val component = ReactComponentB[Unit]("Use")
    .render { $ =>
      val html = org.scalajs.dom.document.getElementById("use").innerHTML
      <.span(^.dangerouslySetInnerHtml(html))
    }
    .build
}
