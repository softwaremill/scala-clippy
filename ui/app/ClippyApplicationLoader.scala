import api.UiApiImpl
import com.softwaremill.id.DefaultIdGenerator
import controllers._
import dal.AdvicesRepository
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n.I18nComponents
import play.api.mvc.EssentialFilter
import router.Routes
import util.{DatabaseConfig, SqlDatabase}
import util.email.{DummyEmailService, SendgridEmailService}

class ClippyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    Logger.configure(context.environment)
    val c = new ClippyComponents(context)
    c.database.updateSchema()
    c.application
  }
}

class ClippyComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with I18nComponents {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val router = new Routes(httpErrorHandler, applicationController, assets, webJarAssets,
    advicesController, autowireController)

  lazy val contactEmail = configuration.getString("email.contact").getOrElse("?")
  lazy val emailService = SendgridEmailService.createFromEnv(contactEmail)
    .getOrElse(new DummyEmailService)

  lazy val webJarAssets = new WebJarAssets(httpErrorHandler, configuration, environment)
  lazy val assets = new controllers.Assets(httpErrorHandler)

  lazy val idGenerator = new DefaultIdGenerator()

  lazy val applicationController = new ApplicationController()

  lazy val database = SqlDatabase.create(new DatabaseConfig { override val rootConfig = configuration.underlying })
  lazy val advicesRepository = new AdvicesRepository(database, idGenerator)

  lazy val uiApiImpl = new UiApiImpl(advicesRepository, emailService, contactEmail)
  lazy val autowireController = new AutowireController(uiApiImpl)

  lazy val advicesController = new AdvicesController(advicesRepository)

  override lazy val httpFilters: Seq[EssentialFilter] = List(new HttpsFilter())
}