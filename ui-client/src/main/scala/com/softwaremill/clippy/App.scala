package com.softwaremill.clippy

import japgolly.scalajs.react._
import autowire._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.concurrent.ExecutionContext.Implicits.global

object App {
  sealed trait Page
  case object ContributeStep1 extends Page
  case class ContributeParseError(errorText: String) extends Page
  case class ContributeStep2(ce: CompilationError) extends Page

  case class State(page: Page, errorMsgs: List[String], infoMsgs: List[String])

  class Backend($: BackendScope[Unit, State]) {
    private val handleReset: Callback = clearMsgs >> $.modState(_.copy(page = ContributeStep1))

    private def handleErrorTextSubmitted(errorText: String): Callback = {
      CompilationErrorParser.parse(errorText) match {
        case None => clearMsgs >> $.modState(_.copy(page = ContributeParseError(errorText)))
        case Some(ce) => clearMsgs >> $.modState(_.copy(page = ContributeStep2(ce)))
      }
    }

    private def handleSendParseError(errorText: String)(email: String): Callback = {
      CallbackTo(AutowireClient[ContributeApi].sendCannotParse(errorText, email).call()).map(
        _.onSuccess {
          case _ =>
            (clearMsgs >> $.modState(s => s.copy(
              page = ContributeStep1,
              infoMsgs = "Error submitted successfully! We'll get in touch soon." :: s.infoMsgs
            ))).runNow()
        }
      )
    }

    private def handleShowError(error: String): Callback = {
      $.modState(s => s.copy(errorMsgs = error :: s.errorMsgs))
    }

    private def clearMsgs = $.modState(_.copy(errorMsgs = Nil, infoMsgs = Nil))

    private def showMsgs(s: State) = <.span(
      s.infoMsgs.map(m => <.div(^.cls := "alert alert-success", ^.role := "alert")(m)) ++
        s.errorMsgs.map(m => <.div(^.cls := "alert alert-danger", ^.role := "alert")(m)): _*
    )

    private def showPage(s: State) = s.page match {
      case ContributeStep1 =>
        Contribute.Step1.component(Contribute.Step1.Props(handleErrorTextSubmitted, handleShowError))

      case ContributeParseError(et) =>
        Contribute.ParseError.component(Contribute.ParseError.Props(handleReset, handleSendParseError(et), handleShowError))

      case ContributeStep2(_) =>
        Contribute.Step2.component()
    }

    def render(s: State) = <.span(
      showMsgs(s),
      showPage(s)
    )
  }
}
