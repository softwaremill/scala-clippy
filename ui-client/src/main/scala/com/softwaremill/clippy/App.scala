package com.softwaremill.clippy

import japgolly.scalajs.react._
import autowire._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.concurrent.ExecutionContext.Implicits.global

object App {
  sealed trait Page
  case object UsePage extends Page
  case object ContributeStep1 extends Page
  case class ContributeParseError(errorText: String) extends Page
  case class ContributeStep2(ce: CompilationError[ExactOrRegex]) extends Page
  case object ListingPage extends Page

  case class State(page: Page, errorMsgs: List[String], infoMsgs: List[String])

  class Backend($: BackendScope[Unit, State]) {
    private val handleReset: Callback = clearMsgs >> $.modState(_.copy(page = ContributeStep1))

    private def handleErrorTextSubmitted(errorText: String): Callback = {
      CompilationErrorParser.parse(errorText) match {
        case None => clearMsgs >> $.modState(_.copy(page = ContributeParseError(errorText)))
        case Some(ce) => clearMsgs >> $.modState(_.copy(page = ContributeStep2(ce.asExactOrRegex)))
      }
    }

    private def handleSendParseError(errorText: String)(email: String): Callback = {
      CallbackTo(AutowireClient[UiApi].sendCannotParse(errorText, email).call()).map(
        _.onSuccess {
          case _ =>
            (clearMsgs >> $.modState(s => s.copy(
              page = ContributeStep1,
              infoMsgs = "Error submitted successfully! We'll get in touch soon." :: s.infoMsgs
            ))).runNow()
        }
      )
    }

    private def handleSendAdviceProposal(ap: AdviceProposal): Callback = {
      CallbackTo(AutowireClient[UiApi].sendAdviceProposal(ap).call()).map(
        _.onSuccess {
          case _ =>
            (clearMsgs >> $.modState(s => s.copy(
              page = ContributeStep1,
              infoMsgs = "Advice submitted successfully! We'll get in touch soon, and let you know when your proposal is accepted." :: s.infoMsgs
            ))).runNow()
        }
      )
    }

    private def handleShowError(error: String): Callback = {
      clearMsgs >> $.modState(s => s.copy(errorMsgs = error :: s.errorMsgs))
    }

    private def clearMsgs = $.modState(_.copy(errorMsgs = Nil, infoMsgs = Nil))

    private def handleSwitchPage(newPage: Page): Callback = {
      clearMsgs >> $.modState { s =>
        def isContribute(p: Page) = p != UsePage
        if (s.page == newPage || (isContribute(s.page) && isContribute(newPage))) s else s.copy(page = newPage)
      }
    }

    private def showMsgs(s: State) = <.span(
      s.infoMsgs.map(m => <.div(^.cls := "alert alert-success", ^.role := "alert")(m)) ++
        s.errorMsgs.map(m => <.div(^.cls := "alert alert-danger", ^.role := "alert")(m)): _*
    )

    private def showPage(s: State) = s.page match {
      case UsePage =>
        Use.component()

      case ContributeStep1 =>
        Contribute.Step1.component(Contribute.Step1.Props(handleErrorTextSubmitted, handleShowError))

      case ContributeParseError(et) =>
        Contribute.ParseError.component(Contribute.ParseError.Props(handleReset, handleSendParseError(et), handleShowError))

      case ContributeStep2(ce) =>
        Contribute.Step2.component(Contribute.Step2.Props(ce, handleReset, handleSendAdviceProposal, handleShowError))

      case ListingPage =>
        Listing.component()
    }

    def render(s: State) = <.span(
      Menu.component((s.page, handleSwitchPage)),
      <.div(^.cls := "container")(
        showMsgs(s),
        showPage(s)
      )
    )
  }
}
