package api

import com.softwaremill.clippy.{Contributor, Library, CompilationError, ContributeApi}
import dal.AdvicesRepository
import util.email.EmailService

import scala.concurrent.{ExecutionContext, Future}

class ContributeApiImpl(
    advicesRepository: AdvicesRepository,
    emailService: EmailService,
    contactEmail: String
)(implicit ec: ExecutionContext) extends ContributeApi {

  override def sendCannotParse(errorText: String, contributorEmail: String) = {
    emailService.send(contactEmail, "Unparseable message",
      s"""
         |Contributor email: $contributorEmail
         |Error text:
         |$errorText
       """.stripMargin)
  }

  override def sendAdviceProposal(compilationError: CompilationError, advice: String, library: Library,
    contributor: Contributor, comment: Option[String]): Future[Unit] = {

    advicesRepository
      .store(compilationError, advice, accepted = false, library, contributor, comment)
      .flatMap { a =>
        emailService.send(contactEmail, "New advice proposal", s"Advice proposal: $a")
      }
  }
}
