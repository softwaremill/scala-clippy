package com.softwaremill.clippy

case class StoredAdvice(id: Long, compilationError: CompilationError, advice: String, accepted: Boolean,
  library: Library, contributor: Contributor, comment: Option[String])
