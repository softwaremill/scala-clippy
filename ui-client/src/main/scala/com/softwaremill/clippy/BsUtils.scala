package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

object BsUtils {
  def bsPanel(body: TagMod*) = <.div(^.cls := "panel panel-default") {
    <.div(^.cls := "panel-body")(body)
  }

  // Maybe this could be a component? But a function works for now
  def bsFormEl(ev: ExternalVar[FormField])(body: Seq[TagMod] => ReactTag) = {
    val formField = ev.value
    val elId = Utils.randomString(8)
    <.div(^.cls := "form-group", formField.error ?= (^.cls := "has-error"))(
      <.label(^.htmlFor := elId, if (formField.required) <.strong(formField.label) else formField.label),
      body(Seq(
        ^.id := elId,
        formField.required ?= (^.required := "required"),
        ^.value := formField.v,
        ^.onChange ==> ((e: ReactEventI) => ev.set(formField.copy(v = e.target.value)))
      ))
    )
  }
}
