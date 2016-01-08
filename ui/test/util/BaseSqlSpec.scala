package util

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

trait BaseSqlSpec extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures {

  private val connectionString = "jdbc:h2:mem:clippy_test" + this.getClass.getSimpleName + ";DB_CLOSE_DELAY=-1"

  lazy val database = SqlDatabase.createEmbedded(connectionString)

  override protected def beforeAll() {
    super.beforeAll()
    createAll()
  }

  override protected def afterAll() {
    super.afterAll()
    dropAll()
    database.close()
  }

  private def dropAll() {
    import database.driver.api._
    database.db.run(sqlu"DROP ALL OBJECTS").futureValue
  }

  private def createAll() {
    database.updateSchema()
  }

  override protected def afterEach() {
    try {
      dropAll()
      createAll()
    }
    catch {
      case e: Exception => e.printStackTrace()
    }

    super.afterEach()
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
