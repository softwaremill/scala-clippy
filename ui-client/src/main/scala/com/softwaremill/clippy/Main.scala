package com.softwaremill.clippy

import japgolly.scalajs.react._
import org.scalajs.jquery._
import scala.scalajs.js

object Main extends js.JSApp {
  type HtmlId = String

  def main(): Unit = {
    jQuery(setupUI _)
  }

  def setupUI(): Unit = {
    setupTabSwitching()
    showFirstTab()

    ContributeOld.setup()

    setupReact()
  }

  private def setupTabSwitching(): Unit = {
    jQuery(".nav li a").click { (ev: JQueryEventObject) =>
      jQuery(".tab").hide()
      jQuery(".nav li").removeClass("active")

      val target = jQuery(ev.target).attr("data-target").get
      jQuery(s"#$target").show()
      jQuery(ev.target).parent("li").addClass("active")

      false
    }
  }

  private def showFirstTab(): Unit = {
    jQuery(".nav li a").first().click()
  }

  private def setupReact(): Unit = {
    val mountNode = org.scalajs.dom.document.getElementById("reactnode")

    val app = ReactComponentB[Unit]("App")
      .initialState(App.State(App.ContributeStep1, Nil, Nil))
      .renderBackend[App.Backend]
      .buildU

    ReactDOM.render(app(), mountNode)
  }
}