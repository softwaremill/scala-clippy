package com.softwaremill.clippy

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.extra.ExternalVar
import monocle._

import scala.util.Random

object Utils {
  def randomString(length: Int) = Random.alphanumeric take length mkString ""

  // ExternalVar companion has only methods for creating a var from AccessRD (read direct), here we are reading
  // through callbacks, so we need that extra method
  def externalVar[S, A]($: BackendScope[_, S], s: S, l: Lens[S, A]): ExternalVar[A] =
    ExternalVar(l.get(s))(a => $.modState(l.set(a)))
}
