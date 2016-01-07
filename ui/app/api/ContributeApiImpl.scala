package api

import com.softwaremill.clippy.{Contributor, Library, CompilationError, ContributeApi}
import dal.AdvicesRepository

import scala.concurrent.{ExecutionContext, Future}

class ContributeApiImpl(advicesRepository: AdvicesRepository)(implicit ec: ExecutionContext) extends ContributeApi {
  override def sendCannotParse(errorText: String, contributorEmail: String) = {
    // TODO send us an email

    println("CANNOT PARSE")
    println(errorText)
    println(contributorEmail)
    println("---")
    Future.successful(())
  }

  override def sendAdviceProposal(compilationError: CompilationError, advice: String, library: Library,
    contributor: Contributor, comment: Option[String]): Future[Unit] = {

    advicesRepository.store(compilationError, advice, accepted = false, library, contributor, comment).map(_ => ())
  }
}
