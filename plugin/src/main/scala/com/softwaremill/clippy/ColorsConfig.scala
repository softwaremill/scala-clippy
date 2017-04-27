package com.softwaremill.clippy

sealed trait ColorsConfig

object ColorsConfig {
  case object Disabled extends ColorsConfig

  case class Enabled(
      diff: fansi.Attrs,
      comment: fansi.Attrs,
      `type`: fansi.Attrs,
      literal: fansi.Attrs,
      keyword: fansi.Attrs,
      reset: fansi.Attrs
  ) extends ColorsConfig

  val defaultEnabled = Enabled(
    fansi.Color.Red,
    fansi.Color.Blue,
    fansi.Color.Green,
    fansi.Color.Magenta,
    fansi.Color.Yellow,
    fansi.Attr.Reset
  )
}
