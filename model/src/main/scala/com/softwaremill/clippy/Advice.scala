package com.softwaremill.clippy

case class Advice(id: Long, compilationError: CompilationError[RegexT], advice: String, library: Library) {
  def errMatching: PartialFunction[CompilationError[ExactT], String] = {
    case ce if compilationError.matches(ce) => advice
  }

  def toXml =
    <advice>
      <id>{ id }</id>
      { compilationError.toXml }
      <text>{ advice }</text>
      { library.toXml }
    </advice>
}
