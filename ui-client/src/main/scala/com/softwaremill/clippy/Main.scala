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
    AutowireClient[PersonApi].list().call().foreach {
      _.foreach { person =>
        val name = jQuery("<div>").addClass("name").text(person.name)
        val age = jQuery("<div>").addClass("age").text(person.age.toString)
        jQuery("#persons").append(jQuery("<li>").append(name).append(age))
      }
    }
  }
}