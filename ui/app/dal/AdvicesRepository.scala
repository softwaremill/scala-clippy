package dal

import com.softwaremill.clippy._
import com.softwaremill.id.IdGenerator
import util.SqlDatabase

import scala.concurrent.{Future, ExecutionContext}

class AdvicesRepository(database: SqlDatabase, idGenerator: IdGenerator)(implicit ec: ExecutionContext) {
  import database._
  import database.driver.api._

  private class AdvicesTable(tag: Tag) extends Table[StoredAdvice](tag, "advices") {
    def id = column[Long]("id", O.PrimaryKey)
    def compilationError = column[String]("compilation_error")
    def advice = column[String]("advice")
    def accepted = column[Boolean]("accepted")
    def libraryGroupId = column[String]("library_group_id")
    def libraryArtifactId = column[String]("library_artifact_id")
    def libraryVersion = column[String]("library_version")
    def contributorEmail = column[Option[String]]("contributor_email")
    def contributorTwitter = column[Option[String]]("contributor_twitter")
    def contributorGithub = column[Option[String]]("contributor_github")
    def comment = column[Option[String]]("comment")

    def * = (id, compilationError, advice, accepted,
      (libraryGroupId, libraryArtifactId, libraryVersion),
      (contributorEmail, contributorGithub, contributorTwitter),
      comment).shaped <> (
        { t => StoredAdvice(t._1, CompilationError.fromXmlString(t._2).get, t._3, t._4, (Library.apply _).tupled(t._5), Contributor.tupled(t._6), t._7) },
        { (a: StoredAdvice) => Some((a.id, a.compilationError.toXmlString, a.advice, a.accepted, Library.unapply(a.library).get, Contributor.unapply(a.contributor).get, a.comment)) }
      )
  }

  private val advices = TableQuery[AdvicesTable]

  def store(compilationError: CompilationError[ExactOrRegex], advice: String, accepted: Boolean, library: Library,
    contributor: Contributor, comment: Option[String]): Future[StoredAdvice] = {

    val a = StoredAdvice(idGenerator.nextId(), compilationError, advice, accepted, library, contributor, comment)

    db.run(advices += a).map(_ => a)
  }

  def findAll(): Future[Seq[StoredAdvice]] = {
    db.run(advices.result)
  }
}