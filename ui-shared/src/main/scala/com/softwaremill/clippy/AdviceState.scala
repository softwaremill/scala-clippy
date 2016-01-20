package com.softwaremill.clippy

object AdviceState extends Enumeration {
  type AdviceState = Value
  val Pending, Accepted, Rejected = Value
}
