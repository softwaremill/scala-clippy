import api.ContributeApiImpl
import com.softwaremill.id.DefaultIdGenerator
import controllers._
import dal.AdvicesRepository
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.db.slick.{DbName, SlickComponents}
import play.api.i18n.I18nComponents
import slick.driver.JdbcProfile
import router.Routes
import util.SqlDatabase

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

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val router = new Routes(httpErrorHandler, applicationController, assets, webJarAssets,
    autowireController)

  lazy val webJarAssets = new WebJarAssets(httpErrorHandler, configuration, environment)
  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val idGenerator = new DefaultIdGenerator()

  lazy val applicationController = new ApplicationController()

  lazy val database = SqlDatabase.fromConfig(api.dbConfig[JdbcProfile](DbName("default")))
  lazy val advicesRepository = new AdvicesRepository(database, idGenerator)

  lazy val contributeApiImpl = new ContributeApiImpl(advicesRepository)
  lazy val autowireController = new AutowireController(contributeApiImpl)

  lazy val dynamicEvolutions = new DynamicEvolutions
}