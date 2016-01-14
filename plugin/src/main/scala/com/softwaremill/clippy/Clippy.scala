package com.softwaremill.clippy

import scala.xml.NodeSeq

case class Clippy(version: String, advices: List[Advice]) {
  def checkPluginVersion(logWarn: String => Unit) = {
    val ourVersion = ClippyBuildInfo.version

    if (version != ourVersion) {
      logWarn(s"New version of clippy plugin available: $version. Please update!")
    }
  }

}

object Clippy {
  def fromXml(xml: NodeSeq): Clippy = {
    val version = xml \\ "clippy" \@ "version"

    val advices = (xml \\ "advice").flatMap { n =>
      for {
        ce <- CompilationError.fromXml(n)
        l <- Library.fromXml(n)
        text = (n \ "text").text
        id = (n \ "id").text.toLong
      } yield Advice(id, ce, text, l)
    }.toList

    Clippy(version, advices)
  }
}
