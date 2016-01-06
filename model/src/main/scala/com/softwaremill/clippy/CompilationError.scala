package com.softwaremill.clippy

sealed trait CompilationError
case class TypeMismatchError(found: String, required: String) extends CompilationError {
  override def toString = s"Type mismatch error.\nFound: $found,\nrequired: $required"
}
case class NotFoundError(what: String) extends CompilationError {
  override def toString = s"Not found error: $what"
}

object CompilationErrorParser {
  private val FoundRegexp = """found\s*:\s*([^\n]+)\n""".r
  private val RequiredRegexp = """required\s*:\s*(.+)""".r
  private val NotFoundRegexp = """not found\s*:\s*(.+)""".r

  def parse(error: String): Option[CompilationError] = {
    if (error.startsWith("type mismatch")) {
      for {
        found <- FoundRegexp.findFirstMatchIn(error)
        required <- RequiredRegexp.findFirstMatchIn(error)
      } yield TypeMismatchError(found.group(1), required.group(1))
    }
    else if (error.startsWith("not found")) {
      for {
        what <- NotFoundRegexp.findFirstMatchIn(error)
      } yield NotFoundError(what.group(1))
    }
    else None
  }
}
