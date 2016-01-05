package com.softwaremill.clippy

import scala.concurrent.Future

trait PersonApi {
  def list(): Future[Seq[Person]]
}
