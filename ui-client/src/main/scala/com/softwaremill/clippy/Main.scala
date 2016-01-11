package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.jquery._
import scala.scalajs.js
import autowire._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends js.JSApp {
  type HtmlId = String

  def main(): Unit = {
    jQuery(setupUI _)
  }

  def setupUI(): Unit = {
    setupTabSwitching()
    showFirstTab()

    Contribute.setup()

    setupReact()
  }

  private def setupTabSwitching(): Unit = {
    jQuery(".nav li a").click { (ev: JQueryEventObject) =>
      jQuery(".tab").hide()
      jQuery(".nav li").removeClass("active")

      val target = jQuery(ev.target).attr("data-target").get
      jQuery(s"#$target").show()
      jQuery(ev.target).parent("li").addClass("active")

      false
    }
  }

  private def showFirstTab(): Unit = {
    jQuery(".nav li a").first().click()
  }

  case class FormField(label: String, required: Boolean, v: String, error: Boolean) {
    def withV(vv: String): FormField = copy(v = vv)
    def validated = if (required) {
      if (v.isEmpty) copy(error = true) else copy(error = false)
    }
    else this
  }
  object FormField {
    def initial(label: String, required: Boolean) = FormField(label, required, "", error = false)
    def errorMsgIfAny(fields: Seq[FormField]): Option[String] = {
      fields.find(_.error).map(ff => s"${ff.label} is required") // required is the only type of error there could be
    }

    def submitValidated[P, S: Validatable](
      $: BackendScope[P, S],
      showError: String => Callback
    )(submit: S => Callback)(e: ReactEventI): Callback = for {
      _ <- e.preventDefaultCB
      props <- $.props
      s <- $.state
      v = implicitly[Validatable[S]]
      s2 = v.validated(s)
      _ <- $.setState(s2)
      fields = v.fields(s2)
      em = errorMsgIfAny(fields)
      _ <- em.fold(submit(s2))(showError)
    } yield ()
  }

  trait Validatable[S] {
    def validated(s: S): S
    def fields(s: S): Seq[FormField]
  }

  sealed trait AppPage
  case object ContributeStep1 extends AppPage
  case class ContributeParseError(errorText: String) extends AppPage
  case class ContributeStep2(ce: CompilationError) extends AppPage

  case class AppState(page: AppPage, errorMsgs: List[String], infoMsgs: List[String])

  private def setupReact(): Unit = {
    val mountNode = org.scalajs.dom.document.getElementById("reactnode")

    //

    def bsPanel(body: TagMod*) = <.div(^.cls := "panel panel-default") {
      <.div(^.cls := "panel-body")(body)
    }

    // TODO -> component
    def bsFormEl(formField: FormField, update: FormField => Callback)(body: => ReactTag) = {
      val elId = Utils.randomString(8)
      <.div(^.cls := "form-group", formField.error ?= (^.cls := "has-error"))(
        <.label(^.htmlFor := elId, if (formField.required) <.strong(formField.label) else formField.label),
        body(
          ^.id := elId,
          formField.required ?= (^.required := "required"),
          ^.value := formField.v,
          ^.onChange ==> ((e: ReactEventI) => update(formField.copy(v = e.target.value)))
        )
      )
    }

    //

    //

    case class ContributeStep1Props(submit: String => Callback, showError: String => Callback)

    case class ContributeStep1State(errorText: FormField)
    implicit val contributeStep1StateVal = new Validatable[ContributeStep1State] {
      override def validated(s: ContributeStep1State) = s.copy(errorText = s.errorText.validated)
      override def fields(s: ContributeStep1State) = List(s.errorText)
    }

    class ContributeStep1Backend($: BackendScope[ContributeStep1Props, ContributeStep1State]) {
      def render(s: ContributeStep1State, p: ContributeStep1Props) = <.div(
        bsPanel(
          <.p("Scala Clippy is only as good as its advice database. Help other users by submitting a fix for a compilation error that you have encountered!"),
          <.p("First, paste in the error and we'll see if we can parse it. Only the error message is needed, without the file name and code snippet, e.g.:"),
          <.pre(
            """type mismatch;
              |found   : akka.http.scaladsl.server.StandardRoute
              |required: akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]""".stripMargin
          )
        ),
        <.form(
          ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => p.submit(s.errorText.v)),
          bsFormEl(s.errorText, net => $.modState(s => s.copy(errorText = net)))(
            <.textarea(^.cls := "form-control", ^.rows := 3)
          ),
          <.button(^.`type` := "submit", ^.cls := "btn btn-primary")("Next")
        )
      )
    }

    val contributeStep1 = ReactComponentB[ContributeStep1Props]("ContributeStep1")
      .initialState(ContributeStep1State(FormField.initial("Error text", required = true)))
      .renderBackend[ContributeStep1Backend]
      .build

    //

    case class ContributeParseErrorProps(reset: Callback, send: String => Callback, showError: String => Callback)

    case class ContributeParseErrorState(email: FormField)
    implicit val contributeParseErrorStateVal = new Validatable[ContributeParseErrorState] {
      override def validated(s: ContributeParseErrorState) = s.copy(email = s.email.validated)
      override def fields(s: ContributeParseErrorState) = List(s.email)
    }

    class ContributeParseErrorBackend($: BackendScope[ContributeParseErrorProps, ContributeParseErrorState]) {
      def render(s: ContributeParseErrorState, p: ContributeParseErrorProps) = <.div(
        <.div(
          bsPanel(
            <.p("Unfortunately we cannot parse the error. Let us know how to contact you, we'll try to find out what's wrong and get back to you.")
          ),
          <.form(
            ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => p.send(s.email.v)),
            bsFormEl(s.email, ne => $.modState(s => s.copy(email = ne)))(
              <.input(^.`type` := "email", ^.cls := "form-control", ^.placeholder := "scalacoder@company.com")
            ),
            <.button(^.`type` := "reset", ^.cls := "btn btn-default", ^.onClick --> p.reset)("Reset"),
            <.button(^.`type` := "submit", ^.cls := "btn btn-primary")("Send")
          )
        )
      )
    }

    val contributeParseError = ReactComponentB[ContributeParseErrorProps]("ContributeParseError")
      .initialState(ContributeParseErrorState(FormField.initial("Email", required = true)))
      .renderBackend[ContributeParseErrorBackend]
      .build

    //

    val contributeStep2 = ReactComponentB[Unit]("ContributeStep2")
      .render { $ => <.div("Parsed!") }
      .buildU

    //

    class AppBackend($: BackendScope[Unit, AppState]) {
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

      private def showMsgs(s: AppState) = <.span(
        s.infoMsgs.map(m => <.div(^.cls := "alert alert-success", ^.role := "alert")(m)) ++
          s.errorMsgs.map(m => <.div(^.cls := "alert alert-danger", ^.role := "alert")(m)): _*
      )

      private def showPage(s: AppState) = s.page match {
        case ContributeStep1 =>
          contributeStep1(ContributeStep1Props(handleErrorTextSubmitted, handleShowError))

        case ContributeParseError(et) =>
          contributeParseError(ContributeParseErrorProps(handleReset, handleSendParseError(et), handleShowError))

        case ContributeStep2(_) =>
          contributeStep2()
      }

      def render(s: AppState) = <.span(
        showMsgs(s),
        showPage(s)
      )
    }

    val app = ReactComponentB[Unit]("App")
      .initialState(AppState(ContributeStep1, Nil, Nil))
      .renderBackend[AppBackend]
      .buildU

    ReactDOM.render(app(), mountNode)
  }
}