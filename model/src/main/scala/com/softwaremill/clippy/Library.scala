package com.softwaremill.clippy

import scala.xml.NodeSeq

case class Library(groupId: String, artifactId: String, version: String) {
  def toXml: NodeSeq = <library>
    <groupId>{groupId}</groupId>
    <artifactId>{artifactId}</artifactId>
    <version>{version}</version>
  </library>
}

object Library {
  def fromXml(xml: NodeSeq): Option[Library] = {
    (xml \\ "library").headOption.map { n =>
      Library(
        (n \ "groupId").text,
        (n \ "artifactId").text,
        (n \ "version").text
      )
    }
  }
}
