package dal

import com.softwaremill.clippy.AdviceState.AdviceState
import com.softwaremill.clippy._
import com.softwaremill.id.IdGenerator
import util.SqlDatabase

import scala.concurrent.{Future, ExecutionContext}

class AdvicesRepository(database: SqlDatabase, idGenerator: IdGenerator)(implicit ec: ExecutionContext) {
  import database._
  import database.driver.api._

  private class AdvicesTable(tag: Tag) extends Table[StoredAdvice](tag, "advices") {
    def id = column[Long]("id", O.PrimaryKey)
    def errorTextRaw = column[String]("error_text_raw")
    def compilationError = column[String]("compilation_error")
    def advice = column[String]("advice")
    def state = column[Int]("state")
    def libraryGroupId = column[String]("library_group_id")
    def libraryArtifactId = column[String]("library_artifact_id")
    def libraryVersion = column[String]("library_version")
    def contributorEmail = column[Option[String]]("contributor_email")
    def contributorTwitter = column[Option[String]]("contributor_twitter")
    def contributorGithub = column[Option[String]]("contributor_github")
    def comment = column[Option[String]]("comment")

    def * = (id, errorTextRaw, compilationError, advice, state,
      (libraryGroupId, libraryArtifactId, libraryVersion),
      (contributorEmail, contributorGithub, contributorTwitter),
      comment).shaped <> (
        { t => StoredAdvice(t._1, t._2, CompilationError.fromXmlString(t._3).get, t._4, AdviceState(t._5), (Library.apply _).tupled(t._6), Contributor.tupled(t._7), t._8) },
        { (a: StoredAdvice) => Some((a.id, a.errorTextRaw, a.compilationError.toXmlString, a.advice, a.state.id, Library.unapply(a.library).get, Contributor.unapply(a.contributor).get, a.comment)) }
      )
  }

  private val advices = TableQuery[AdvicesTable]

  def store(errorTextRaw: String, compilationError: CompilationError[RegexT], advice: String,
    state: AdviceState, library: Library, contributor: Contributor, comment: Option[String]): Future[StoredAdvice] = {

    val a = StoredAdvice(idGenerator.nextId(), errorTextRaw, compilationError, advice, state, library,
      contributor, comment)

    db.run(advices += a).map(_ => a)
  }

  def findAll(): Future[Seq[StoredAdvice]] = {
    db.run(advices.result)
  }
}

case class StoredAdvice(id: Long, errorTextRaw: String, compilationError: CompilationError[RegexT],
    advice: String, state: AdviceState, library: Library, contributor: Contributor, comment: Option[String]) {

  def toAdvice = Advice(id, compilationError, advice, library)
  def toAdviceListing = AdviceListing(id, compilationError, advice, library,
    ContributorListing(contributor.github, contributor.twitter))
}
