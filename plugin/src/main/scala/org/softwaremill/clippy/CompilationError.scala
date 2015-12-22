package org.softwaremill.clippy

sealed trait CompilationError
case class TypeMismatchError(found: String, required: String) extends CompilationError

object CompilationErrorParser {
  private val FoundRegexp = """found\s*:\s*([^\n]+)\n""".r
  private val RequiredRegexp = """required\s*:\s*([^\n]+)\n""".r

  def parse(error: String): Option[CompilationError] = {
    if (error.startsWith("type mismatch")) {
      for {
        found <- FoundRegexp.findFirstMatchIn(error)
        required <- RequiredRegexp.findFirstMatchIn(error)
      } yield TypeMismatchError(found.group(1), required.group(1)``)
    } else None
  }
}
