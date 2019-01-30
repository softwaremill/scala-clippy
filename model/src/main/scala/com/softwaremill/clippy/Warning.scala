package com.softwaremill.clippy

import org.json4s.JsonAST._

final case class Warning(pattern: RegexT, text: Option[String]) {
  def toJson: JValue =
    JObject(
      "pattern" -> JString(pattern.toString)
    ) ++ text.map(t => JObject("text" -> JString(t))).getOrElse(JNothing)
}

object Warning {
  def fromJson(jvalue: JValue): Option[Warning] =
    (for {
      JObject(fields)                        <- jvalue
      JField("pattern", JString(patternStr)) <- fields
      pattern = RegexT.fromRegex(patternStr)

    } yield {
      val text = jvalue.findField {
        case (("text", _)) => true
        case _             => false
      } match {
        case Some((_, JString(textStr))) => Some(textStr)
        case _                           => None
      }
      Warning(pattern, text)
    }).headOption
}
