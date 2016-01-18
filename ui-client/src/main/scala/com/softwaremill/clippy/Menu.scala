package com.softwaremill.clippy

import com.softwaremill.clippy.App.{ListingPage, ContributeStep1, UsePage, Page}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Menu {
  val component = ReactComponentB[(Page, Page => Callback)]("Menu")
    .render { $ =>
      val isUsePage = $.props._1 == UsePage
      val isListingPage = $.props._1 == ListingPage
      val isContributePage = !isUsePage && !isListingPage
      def switchTo(p: Page)(e: ReactEventI) = e.preventDefaultCB >> $.props._2(p)

      <.nav(^.cls := "navbar navbar-inverse navbar-fixed-top")(
        <.div(^.cls := "container")(
          <.div(^.cls := "navbar-header")(
            <.button(^.`type` := "button", ^.cls := "navbar-toggle collapsed", "data-toggle".reactAttr := "collapse",
              "data-target".reactAttr := "navbar", "aria-expanded".reactAttr := "false",
              "aria-controls".reactAttr := "navbar")(
                <.span(^.cls := "sr-only")("Toggle navigation"),
                <.span(^.cls := "icon-bar"),
                <.span(^.cls := "icon-bar"),
                <.span(^.cls := "icon-bar")
              ),
            <.a(^.cls := "navbar-brand", ^.onClick ==> switchTo(UsePage), ^.href := "#")("Scala Clippy")
          ),
          <.div(^.id := "navbar", ^.cls := "collapse navbar-collapse")(
            <.ul(^.cls := "nav navbar-nav")(
              <.li(isUsePage ?= (^.cls := "active"))(
                <.a("Use", ^.onClick ==> switchTo(UsePage), ^.href := "#")
              ),
              <.li(isContributePage ?= (^.cls := "active"))(
                <.a("Contribute", ^.onClick ==> switchTo(ContributeStep1), ^.href := "#")
              ),
              <.li(isListingPage ?= (^.cls := "active"))(
                <.a("Listing", ^.onClick ==> switchTo(ListingPage), ^.href := "#")
              )
            )
          )
        )
      )
    }
    .build
}
