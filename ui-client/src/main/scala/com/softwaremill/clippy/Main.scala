package com.softwaremill.clippy

import org.scalajs.jquery._

import scala.scalajs.js

object Main extends js.JSApp {
  def main(): Unit = {
    println("AAA")
    jQuery(setupUI _)
  }

  def setupUI(): Unit = {
    jQuery.getJSON(
      "/persons",
      success = { data: js.Any =>
        jQuery.each(data, { (index: js.Any, anyPerson: js.Any) =>
          val person = anyPerson.asInstanceOf[Person]
          val name = jQuery("<div>").addClass("name").text(person.name)
          val age = jQuery("<div>").addClass("age").text(person.age.toString)
          jQuery("#persons").append(jQuery("<li>").append(name).append(age))
          (): js.Any
        })
      }
    )
  }
}

@js.native
trait Person extends js.Object {
  val name: String = js.native
  val age: Int = js.native
}