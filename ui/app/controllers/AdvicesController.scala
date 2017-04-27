package controllers

import com.softwaremill.clippy.{Advice, Clippy}
import dal.AdvicesRepository
import play.api.mvc.{Action, Controller}
import util.{ClippyBuildInfo, Zip}

import scala.concurrent.{ExecutionContext, Future}

class AdvicesController(advicesRepository: AdvicesRepository)(implicit ec: ExecutionContext) extends Controller {
  def get = Action.async {
    gzippedAdvices.map(a => Ok(a).withHeaders("Content-Encoding" -> "gzip"))
  }

  def gzippedAdvices: Future[Array[Byte]] =
    advicesRepository.findAll().map { storedAdvices =>
      // TODO, once there's an admin: filter out not accepted advice
      val advices = storedAdvices
      //.filter(_.accepted)
        .map(_.toAdvice)
      Zip.compress(toJsonString(advices.toList))
    }

  private def toJsonString(advices: List[Advice]): String = {
    import org.json4s.native.JsonMethods.{render => r, compact}
    compact(r(Clippy(ClippyBuildInfo.version, advices).toJson))
  }
}
