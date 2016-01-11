package com.softwaremill.clippy

import japgolly.scalajs.react._

case class FormField(label: String, required: Boolean, v: String, error: Boolean) {
  def withV(vv: String): FormField = copy(v = vv)
  def validated = if (required) {
    if (v.isEmpty) copy(error = true) else copy(error = false)
  }
  else this
}
object FormField {
  def initial(label: String, required: Boolean) = FormField(label, required, "", error = false)
  def errorMsgIfAny(fields: Seq[FormField]): Option[String] = {
    fields.find(_.error).map(ff => s"${ff.label} is required") // required is the only type of error there could be
  }

  def submitValidated[P, S: Validatable](
    $: BackendScope[P, S],
    showError: String => Callback
  )(submit: S => Callback)(e: ReactEventI): Callback = for {
    _ <- e.preventDefaultCB
    props <- $.props
    s <- $.state
    v = implicitly[Validatable[S]]
    s2 = v.validated(s)
    _ <- $.setState(s2)
    fields = v.fields(s2)
    em = errorMsgIfAny(fields)
    _ <- em.fold(submit(s2))(showError)
  } yield ()
}

trait Validatable[S] {
  def validated(s: S): S
  def fields(s: S): Seq[FormField]
}