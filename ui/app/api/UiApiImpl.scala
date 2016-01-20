package api

import com.softwaremill.clippy._
import dal.AdvicesRepository
import util.email.EmailService

import scala.concurrent.{ExecutionContext, Future}

class UiApiImpl(
    advicesRepository: AdvicesRepository,
    emailService: EmailService,
    contactEmail: String
)(implicit ec: ExecutionContext) extends UiApi {

  override def sendCannotParse(errorText: String, contributorEmail: String) = {
    emailService.send(contactEmail, "Clippy: unparseable message",
      s"""
         |Contributor email: $contributorEmail
         |
         |Error text:
         |$errorText
       """.stripMargin)
  }

  override def sendAdviceProposal(ap: AdviceProposal): Future[Unit] = {
    advicesRepository
      .store(ap.errorTextRaw, ap.compilationError, ap.advice, AdviceState.Pending, ap.library, ap.contributor, ap.comment)
      .flatMap { a =>
        emailService.send(contactEmail, "Clippy: new advice proposal",
          s"""
             |Advice proposal:
             |$a
             |""".stripMargin)
      }
  }

  override def listAccepted() = {
    advicesRepository.findAll().map(_.map(_.toAdviceListing))
  }

  override def sendSuggestEdit(text: String, contactEmail: String, adviceListing: AdviceListing) = {
    emailService.send(contactEmail, "Clippy: edit suggestion",
      s"""
         |Edit suggestion for: $adviceListing
         |Contact email: $contactEmail
         |
         |Suggestion:
         |$text
       """.stripMargin)
  }

  override def feedback(text: String, contactEmail: String) = {
    emailService.send(contactEmail, "Clippy: feedback",
      s"""
         |Contact email: $contactEmail
         |
         |Feedback:
         |$text
       """.stripMargin)
  }
}
