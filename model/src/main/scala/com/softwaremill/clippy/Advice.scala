package com.softwaremill.clippy

case class Advice(id: Long, compilationError: CompilationError[ExactOrRegex], advice: String, library: Library) {
  def errMatching: PartialFunction[CompilationError[Exact], String] = {
    case ce if compilationError.matches(ce) => advice
  }
}
