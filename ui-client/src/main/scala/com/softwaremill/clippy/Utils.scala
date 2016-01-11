package com.softwaremill.clippy

import scala.util.Random

object Utils {
  def randomString(length: Int) = Random.alphanumeric take length mkString ""
}
