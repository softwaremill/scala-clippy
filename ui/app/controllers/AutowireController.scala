package controllers

import com.softwaremill.clippy.PersonApi
import play.api.mvc.{Action, Controller}

import upickle.default._
import upickle.Js

import scala.concurrent.ExecutionContext.Implicits.global

class AutowireController(personApi: PersonApi) extends Controller {

  def autowireApi(path: String) = Action.async {
    implicit request =>
      val b = request.body.asText.getOrElse("")

      AutowireServer.route[PersonApi](personApi)(
        autowire.Core.Request(
          path.split("/"),
          upickle.json.read(b).asInstanceOf[Js.Obj].value.toMap
        )
      ).map(jsv => Ok(upickle.json.write(jsv)))
  }
}

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer] {
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)
  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}