package com.softwaremill.clippy

import org.scalajs.jquery._
import scala.scalajs.js

object Main extends js.JSApp {
  def main(): Unit = {
    jQuery(setupUI _)
  }

  def setupUI(): Unit = {
    setupTabSwitching()
    showFirstTab()

    Contribute.setup()
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
}