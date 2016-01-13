package com.softwaremill.clippy

case class StoredAdvice(id: Long, compilationError: CompilationError[ExactOrRegex], advice: String, accepted: Boolean,
  library: Library, contributor: Contributor, comment: Option[String])
