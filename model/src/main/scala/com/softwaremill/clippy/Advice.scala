package com.softwaremill.clippy

import org.json4s.JsonAST._

case class Advice(id: Long, compilationError: CompilationError[RegexT], advice: String, library: Library) {
  def errMatching: PartialFunction[CompilationError[ExactT], String] = {
    case ce if compilationError.matches(ce) => advice
  }

  def toJson: JValue = JObject(
      "id" -> JInt(id),
      "error" -> compilationError.toJson,
      "text" -> JString(advice),
      "library" -> library.toJson
    )
}

object Advice {
  def fromJson(jvalue: JValue): Option[Advice] = {
    (for {
      JObject(fields) <- jvalue
      JField("id", JInt(id)) <- fields
      JField("error", errorJV) <- fields
      error <- CompilationError.fromJson(errorJV).toList
      JField("text", JString(text)) <- fields
      JField("library", libraryJV) <- fields
      library <- Library.fromJson(libraryJV).toList
    } yield Advice(id.longValue(), error, text, library)).headOption
  }
}
