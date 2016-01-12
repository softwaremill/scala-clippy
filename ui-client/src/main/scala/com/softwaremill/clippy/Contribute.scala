package com.softwaremill.clippy

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import BsUtils._

object Contribute {
  object Step1 {
    case class Props(submit: String => Callback, showError: String => Callback)

    case class State(errorText: FormField)
    implicit val stateVal = new Validatable[State] {
      override def validated(s: State) = s.copy(errorText = s.errorText.validated)
      override def fields(s: State) = List(s.errorText)
    }

    class Backend($: BackendScope[Props, State]) {
      def render(s: State, p: Props) = <.div(
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

    val component = ReactComponentB[Props]("ContributeStep1")
      .initialState(State(FormField("Error text", required = true)))
      .renderBackend[Backend]
      .build
  }

  object Step2 {
    case class Props(ce: CompilationError, reset: Callback, send: AdviceProposal => Callback,
      showError: String => Callback)

    case class State(advice: FormField, libraryGroupId: FormField, libraryArtifactId: FormField,
      libraryVersion: FormField, email: FormField, twitter: FormField, github: FormField, comment: FormField)
    implicit val stateVal = new Validatable[State] {
      override def validated(s: State) = s.copy(
        advice = s.advice.validated,
        libraryGroupId = s.libraryGroupId.validated,
        libraryArtifactId = s.libraryArtifactId.validated,
        libraryVersion = s.libraryVersion.validated,
        email = s.email.validated,
        twitter = s.twitter.validated,
        github = s.github.validated,
        comment = s.comment.validated
      )
      override def fields(s: State) = List(s.advice, s.libraryGroupId, s.libraryArtifactId, s.libraryVersion,
        s.email, s.twitter, s.github, s.comment)
    }

    class Backend($: BackendScope[Props, State]) {
      def render(s: State, p: Props) = <.div(
        bsPanel(
          <.p("Parsing successfull! Here's what we've found:"),
          <.pre(p.ce.toString),
          <.p("You can optionally leave an e-mail so that we can let you know when your proposal is accepted, and a twitter/github handle so that we can add proper attribution, visible in the advice browser.")
        ),
        <.form(
          ^.onSubmit ==> FormField.submitValidated($, p.showError)(s => p.send(AdviceProposal(
            p.ce, s.advice.v, Library(s.libraryGroupId.v, s.libraryArtifactId.v, s.libraryVersion.v),
            Contributor(s.email.vOpt, s.twitter.vOpt, s.github.vOpt), s.comment.vOpt
          ))),
          bsFormEl(s.advice, net => $.modState(s => s.copy(advice = net)))(
            <.textarea(^.cls := "form-control", ^.rows := 3)
          ),
          <.hr,
          bsFormEl(s.libraryGroupId, lg => $.modState(s => s.copy(libraryGroupId = lg)))(
            <.input(^.`type` := "text", ^.cls := "form-control", ^.placeholder := "org.scala")
          ),
          bsFormEl(s.libraryArtifactId, la => $.modState(s => s.copy(libraryArtifactId = la)))(
            <.input(^.`type` := "text", ^.cls := "form-control", ^.placeholder := "scala-lang")
          ),
          bsFormEl(s.libraryVersion, lv => $.modState(s => s.copy(libraryVersion = lv)))(
            <.input(^.`type` := "text", ^.cls := "form-control", ^.placeholder := "2.11-M3")
          ),
          <.hr,
          /* TODO
        <div class="input-group">
                <div class="input-group-addon">&commat;</div>
                <input type="email" class="form-control" id="step2parsedTwitter" placeholder="twitter">
            </div>
         */
          bsFormEl(s.email, e => $.modState(s => s.copy(email = e)))(
            <.input(^.`type` := "email", ^.cls := "form-control", ^.placeholder := "scalacoder@company.com")
          ),
          // TODO as above
          bsFormEl(s.twitter, tw => $.modState(s => s.copy(twitter = tw)))(
            <.input(^.`type` := "text", ^.cls := "form-control", ^.placeholder := "twitter")
          ),
          bsFormEl(s.github, gh => $.modState(s => s.copy(github = gh)))(
            <.input(^.`type` := "text", ^.cls := "form-control", ^.placeholder := "github")
          ),
          <.hr,
          bsFormEl(s.comment, c => $.modState(s => s.copy(comment = c)))(
            <.textarea(^.cls := "form-control", ^.rows := 3)
          ),
          <.button(^.`type` := "reset", ^.cls := "btn btn-default", ^.onClick --> p.reset)("Reset"),
          <.button(^.`type` := "submit", ^.cls := "btn btn-primary")("Send")
        )
      )
    }

    val component = ReactComponentB[Props]("ContributeStep2")
      .initialState(State(
        FormField("Advice", required = true),
        FormField("Library group id", required = true),
        FormField("Library artifact id", required = true),
        FormField("Library version", required = true),
        FormField("E-mail (optional)", required = false),
        FormField("Twitter handle (optional)", required = false),
        FormField("Github handle (optional)", required = false),
        FormField("Comment (optional)", required = false)
      ))
      .renderBackend[Backend]
      .build
  }

  object ParseError {
    case class Props(reset: Callback, send: String => Callback, showError: String => Callback)

    case class State(email: FormField)
    implicit val stateVal = new Validatable[State] {
      override def validated(s: State) = s.copy(email = s.email.validated)
      override def fields(s: State) = List(s.email)
    }

    class Backend($: BackendScope[Props, State]) {
      def render(s: State, p: Props) = <.div(
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
    }

    val component = ReactComponentB[Props]("ContributeParseError")
      .initialState(State(FormField("Email", required = true)))
      .renderBackend[Backend]
      .build
  }
}
