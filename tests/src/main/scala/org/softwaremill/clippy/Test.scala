package org.softwaremill.clippy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

class Test {

  import akka.http.scaladsl.server.Directives._

  implicit val system = ActorSystem()
  //implicit val m = ActorMaterializer()

  val r = complete("ok")

  Http().bindAndHandle(r, "localhost", 8080)
}
