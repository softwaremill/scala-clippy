package com.softwaremill.clippy

import org.scalajs.jquery._
import autowire._

import scala.scalajs.js

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends js.JSApp {
  def main(): Unit = {
    jQuery(setupUI _)
  }

  def setupUI(): Unit = {
    setupTabSwitching()
    showFirstTab()

    populatePeopleList()
    setupAddPerson()
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

  private def populatePeopleList(): Unit = {
    AutowireClient[PersonApi].list().call().foreach {
      jQuery("#people li").remove()
      _.foreach { person =>
        val name = jQuery("<div>").addClass("name").text(person.name)
        val age = jQuery("<div>").addClass("age").text(person.age.toString)
        jQuery("#people").append(jQuery("<li>").append(name).append(age))
      }
    }
  }

  private def setupAddPerson(): Unit = {
    jQuery("#addPerson").click { (ev: JQueryEventObject) =>
      ev.stopImmediatePropagation()

      val name = jQuery("#name").value().asInstanceOf[String]
      val age = jQuery("#age").value().asInstanceOf[String].toInt

      AutowireClient[PersonApi].add(name, age).call().onSuccess {
        case _ =>
          populatePeopleList()
      }

      false
    }
  }
}