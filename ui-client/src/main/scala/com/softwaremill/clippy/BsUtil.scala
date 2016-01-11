package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object BsUtil {
  def bsPanel(body: TagMod*) = <.div(^.cls := "panel panel-default") {
    <.div(^.cls := "panel-body")(body)
  }

  // TODO -> component
  def bsFormEl(formField: FormField, update: FormField => Callback)(body: => ReactTag) = {
    val elId = Utils.randomString(8)
    <.div(^.cls := "form-group", formField.error ?= (^.cls := "has-error"))(
      <.label(^.htmlFor := elId, if (formField.required) <.strong(formField.label) else formField.label),
      body(
        ^.id := elId,
        formField.required ?= (^.required := "required"),
        ^.value := formField.v,
        ^.onChange ==> ((e: ReactEventI) => update(formField.copy(v = e.target.value)))
      )
    )
  }
}
