package com.softwaremill.clippy

import org.json4s.JsonAST.{JField, JObject, JString, JValue}

case class Library(groupId: String, artifactId: String, version: String) {
  def toJson: JValue = JObject(
    "groupId"    -> JString(groupId),
    "artifactId" -> JString(artifactId),
    "version"    -> JString(version)
  )

  override def toString = s"$groupId:$artifactId:$version"
}

object Library {
  def fromJson(jvalue: JValue): Option[Library] =
    (for {
      JObject(fields)                           <- jvalue
      JField("groupId", JString(groupId))       <- fields
      JField("artifactId", JString(artifactId)) <- fields
      JField("version", JString(version))       <- fields
    } yield Library(groupId, artifactId, version)).headOption
}
