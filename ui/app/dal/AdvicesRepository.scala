package dal

import com.softwaremill.clippy.AdviceState.AdviceState
import com.softwaremill.clippy._
import com.softwaremill.id.IdGenerator
import util.SqlDatabase

import scala.concurrent.{ExecutionContext, Future}

class AdvicesRepository(database: SqlDatabase, idGenerator: IdGenerator)(implicit ec: ExecutionContext) {
  import database._
  import database.driver.api._

  private class AdvicesTable(tag: Tag) extends Table[StoredAdvice](tag, "advices") {
    def id                 = column[Long]("id", O.PrimaryKey)
    def errorTextRaw       = column[String]("error_text_raw")
    def patternRaw         = column[String]("pattern_raw")
    def compilationError   = column[String]("compilation_error")
    def advice             = column[String]("advice")
    def state              = column[Int]("state")
    def libraryGroupId     = column[String]("library_group_id")
    def libraryArtifactId  = column[String]("library_artifact_id")
    def libraryVersion     = column[String]("library_version")
    def contributorEmail   = column[Option[String]]("contributor_email")
    def contributorTwitter = column[Option[String]]("contributor_twitter")
    def contributorGithub  = column[Option[String]]("contributor_github")
    def comment            = column[Option[String]]("comment")

    def * =
      (
        id,
        errorTextRaw,
        patternRaw,
        compilationError,
        advice,
        state,
        (libraryGroupId, libraryArtifactId, libraryVersion),
        (contributorEmail, contributorGithub, contributorTwitter),
        comment
      ).shaped <> ({ t =>
        StoredAdvice(
          t._1,
          t._2,
          t._3,
          CompilationError.fromJsonString(t._4).get,
          t._5,
          AdviceState(t._6),
          (Library.apply _).tupled(t._7),
          Contributor.tupled(t._8),
          t._9
        )
      }, { (a: StoredAdvice) =>
        Some(
          (
            a.id,
            a.errorTextRaw,
            a.patternRaw,
            a.compilationError.toJsonString,
            a.advice,
            a.state.id,
            Library.unapply(a.library).get,
            Contributor.unapply(a.contributor).get,
            a.comment
          )
        )
      })
  }

  private val advices = TableQuery[AdvicesTable]

  def store(
      errorTextRaw: String,
      patternRaw: String,
      compilationError: CompilationError[RegexT],
      advice: String,
      state: AdviceState,
      library: Library,
      contributor: Contributor,
      comment: Option[String]
  ): Future[StoredAdvice] = {

    val a = StoredAdvice(
      idGenerator.nextId(),
      errorTextRaw,
      patternRaw,
      compilationError,
      advice,
      state,
      library,
      contributor,
      comment
    )

    db.run(advices += a).map(_ => a)
  }

  def findAll(): Future[Seq[StoredAdvice]] =
    db.run(advices.result)
}

case class StoredAdvice(
    id: Long,
    errorTextRaw: String,
    patternRaw: String,
    compilationError: CompilationError[RegexT],
    advice: String,
    state: AdviceState,
    library: Library,
    contributor: Contributor,
    comment: Option[String]
) {

  def toAdvice = Advice(compilationError, advice, library)
  def toAdviceListing =
    AdviceListing(id, compilationError, advice, library, ContributorListing(contributor.github, contributor.twitter))
}
