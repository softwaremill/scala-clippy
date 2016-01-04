import controllers.{WebJarAssets, PersonController}
import dal.PersonRepository
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.db.slick.{DbName, SlickComponents}
import play.api.i18n.I18nComponents
import slick.driver.JdbcProfile
import router.Routes
import scala.concurrent.ExecutionContext.Implicits.global

class ClippyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    Logger.configure(context.environment)
    val c = new ClippyComponents(context)
    c.applicationEvolutions
    c.application
  }
}

class ClippyComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with SlickComponents
    with I18nComponents
    with EvolutionsComponents
    with SlickEvolutionsComponents {

  lazy val router = new Routes(httpErrorHandler, personController, assets, webJarAssets)

  lazy val webJarAssets = new WebJarAssets(httpErrorHandler, configuration, environment)
  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val dbConfig = api.dbConfig[JdbcProfile](DbName("default"))
  lazy val personReposiotry = new PersonRepository(dbConfig)
  lazy val personController = new PersonController(personReposiotry, messagesApi)

  lazy val dynamicEvolutions = new DynamicEvolutions
}