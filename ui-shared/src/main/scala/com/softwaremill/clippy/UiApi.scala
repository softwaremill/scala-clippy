package com.softwaremill.clippy

import scala.concurrent.Future

trait UiApi extends ContributeApi with ListingApi with FeedbackApi

trait FeedbackApi {
  def feedback(text: String, contactEmail: String): Future[Unit]
}

trait ContributeApi {
  def sendCannotParse(errorText: String, contributorEmail: String): Future[Unit]
  def sendAdviceProposal(adviceProposal: AdviceProposal): Future[Unit]
}

trait ListingApi {
  def listAccepted(): Future[Seq[AdviceListing]]
  def sendSuggestEdit(text: String, contactEmail: String, adviceListing: AdviceListing): Future[Unit]
}

case class AdviceProposal(
    errorTextRaw: String,
    patternRaw: String,
    compilationError: CompilationError[RegexT],
    advice: String,
    library: Library,
    contributor: Contributor,
    comment: Option[String]
)

case class ContributorListing(twitter: Option[String], github: Option[String])

case class AdviceListing(
    id: Long,
    compilationError: CompilationError[RegexT],
    advice: String,
    library: Library,
    contributor: ContributorListing
)
