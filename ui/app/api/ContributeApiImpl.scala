package api

import com.softwaremill.clippy.{Contributor, Library, CompilationError, ContributeApi}

import scala.concurrent.Future

class ContributeApiImpl extends ContributeApi {
  override def sendCannotParse(errorText: String, contributorEmail: String) = {
    // TODO send us an email

    println("CANNOT PARSE")
    println(errorText)
    println(contributorEmail)
    println("---")
    Future.successful(())
  }

  override def sendAdviceProposal(
    compilationError: CompilationError,
    advice: String,
    library: Library,
    contributor: Contributor,
    comment: Option[String]
  ) = {

    // TODO save advice proposal, send email

    println("ADVICE PROPOSAL")
    println(compilationError)
    println(advice)
    println(library)
    println(contributor)
    println(comment)
    println("---")
    Future.successful(())
  }
}
