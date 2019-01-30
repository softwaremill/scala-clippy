package com.softwaremill.clippy

import org.json4s.JsonAST._

case class Clippy(version: String, advices: List[Advice], fatalWarnings: List[Warning]) {
  def checkPluginVersion(ourVersion: String, logInfo: String => Unit) =
    if (version != ourVersion) {
      logInfo(s"New version of clippy plugin available: $version. Please update!")
    }

  def toJson: JValue = JObject(
    "version"       -> JString(version),
    "advices"       -> JArray(advices.map(_.toJson)),
    "fatalWarnings" -> JArray(fatalWarnings.map(_.toJson))
  )
}

object Clippy {
  def fromJson(jvalue: JValue): Option[Clippy] =
    (for {
      JObject(fields)                     <- jvalue
      JField("version", JString(version)) <- fields
    } yield {
      val advices = jvalue.findField {
        case JField("advices", _) => true
        case _                    => false
      } match {
        case Some((_, JArray(advicesJV))) => advicesJV.flatMap(Advice.fromJson)
        case _                            => Nil
      }
      val fatalWarnings = fields.find { tpl =>
        tpl._1 == "fatalWarnings"
      } match {
        case Some((_, JArray(fatalWarningsJV))) => fatalWarningsJV.flatMap(Warning.fromJson)
        case _                                  => Nil
      }
      Clippy(version, advices, fatalWarnings)
    }).headOption
}
