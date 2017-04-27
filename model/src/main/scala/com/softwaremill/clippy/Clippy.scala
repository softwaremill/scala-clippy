package com.softwaremill.clippy

import org.json4s.JsonAST._

case class Clippy(version: String, advices: List[Advice]) {
  def checkPluginVersion(ourVersion: String, logInfo: String => Unit) =
    if (version != ourVersion) {
      logInfo(s"New version of clippy plugin available: $version. Please update!")
    }

  def toJson: JValue = JObject(
    "version" -> JString(version),
    "advices" -> JArray(advices.map(_.toJson))
  )
}

object Clippy {
  def fromJson(jvalue: JValue): Option[Clippy] =
    (for {
      JObject(fields)                      <- jvalue
      JField("version", JString(version))  <- fields
      JField("advices", JArray(advicesJV)) <- fields
    } yield Clippy(version, advicesJV.flatMap(Advice.fromJson))).headOption
}
