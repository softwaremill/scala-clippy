package com.softwaremill.clippy

import japgolly.scalajs.react._
import autowire._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object App {
  sealed trait Page
  case object UsePage extends Page
  case object ContributeStep1 extends Page
  case class ContributeParseError(errorText: String) extends Page
  case class ContributeStep2(errorTextRaw: String, ce: CompilationError[RegexT]) extends Page
  case object ListingPage extends Page
  case object FeedbackPage extends Page

  case class State(page: Page, errorMsgs: List[String], infoMsgs: List[String])

  class Backend($: BackendScope[Unit, State]) {
    private val handleReset: Callback = clearMsgs >> $.modState(_.copy(page = ContributeStep1))

    private def handleErrorTextSubmitted(errorText: String): Callback = {
      CompilationErrorParser.parse(errorText) match {
        case None => clearMsgs >> $.modState(_.copy(page = ContributeParseError(errorText)))
        case Some(ce) => clearMsgs >> $.modState(_.copy(page = ContributeStep2(errorText, ce.asRegex)))
      }
    }

    private def handleSendParseError(errorText: String)(email: String): Callback = {
      handleFuture(
        AutowireClient[UiApi].sendCannotParse(errorText, email).call(),
        Some("Error submitted successfully! We'll get in touch soon."),
        Some((_: Unit) => $.modState(s => s.copy(page = ContributeStep1)))
      )
    }

    private def handleSendAdviceProposal(ap: AdviceProposal): Callback = {
      handleFuture(
        AutowireClient[UiApi].sendAdviceProposal(ap).call(),
        Some("Advice submitted successfully! We'll get in touch soon, and let you know when your proposal is accepted."),
        Some((_: Unit) => $.modState(s => s.copy(page = ContributeStep1)))
      )
    }

    private lazy val handleFuture = new HandleFuture {
      override def apply[T](f: Future[T], successMsg: Option[String], successCallback: Option[(T) => Callback]) =
        CallbackTo(f onComplete {
          case Success(v) =>
            val msgCallback = successMsg map handleShowInfo
            val vCallback = successCallback map (_(v))

            (msgCallback.getOrElse(Callback.empty) >> vCallback.getOrElse(Callback.empty)).runNow()

          case Failure(e) => handleShowError("Error communicating with the server").runNow()
        })
    }

    private def handleShowError(error: String): Callback = {
      clearMsgs >> $.modState(s => s.copy(errorMsgs = error :: s.errorMsgs))
    }

    private def handleShowInfo(info: String): Callback = {
      clearMsgs >> $.modState(s => s.copy(infoMsgs = info :: s.infoMsgs))
    }

    private def clearMsgs = $.modState(_.copy(errorMsgs = Nil, infoMsgs = Nil))

    private def handleSwitchPage(newPage: Page): Callback = {
      clearMsgs >> $.modState { s =>
        def isContribute(p: Page) = p != UsePage && p != ListingPage && p != FeedbackPage
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

      case ContributeStep2(errorTextRaw, ce) =>
        Contribute.Step2.component(Contribute.Step2.Props(errorTextRaw, ce, handleReset, handleSendAdviceProposal, handleShowError))

      case ListingPage =>
        Listing.component(Listing.Props(handleShowError, clearMsgs, handleFuture))

      case FeedbackPage =>
        Feedback.component(Feedback.Props(handleShowError, clearMsgs, handleFuture))
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

trait HandleFuture {
  def apply[T](f: Future[T], successMsg: Option[String], successCallback: Option[T => Callback]): Callback
}