package com.softwaremill.clippy

import scala.concurrent.Future

trait PersonApi {
  def add(name: String, age: Int): Future[Unit]
  def list(): Future[Seq[Person]]
}
