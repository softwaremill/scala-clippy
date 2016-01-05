package com.softwaremill.clippy

import scala.concurrent.Future

trait ContributeApi {
  def sendCannotParse(errorText: String, contributorEmail: String): Future[Unit]
  def sendAdviceProposal(compilationError: CompilationError, advice: String, library: Library,
    contributor: Contributor, comment: Option[String])
}
