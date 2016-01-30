package controllers

import com.softwaremill.clippy.Advice
import dal.AdvicesRepository
import play.api.mvc.{Action, Controller}
import util.{Zip, ClippyBuildInfo}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Utility

class AdvicesController(advicesRepository: AdvicesRepository)(implicit ec: ExecutionContext) extends Controller {
  def get = Action.async {
    gzippedAdvices.map(a => Ok(a).withHeaders("Content-Encoding" -> "gzip"))
  }

  def gzippedAdvices: Future[Array[Byte]] = {
    advicesRepository.findAll().map { storedAdvices =>
      // TODO, once there's an admin: filter out not accepted advice
      val advices = storedAdvices
        //.filter(_.accepted)
        .map(_.toAdvice)
      Zip.compress(toXmlString(advices))
    }
  }

  private def toXmlString(advices: Seq[Advice]): String = {
    val xml = <clippy version={ ClippyBuildInfo.version }>
                { advices.map(_.toXml) }
              </clippy>

    Utility.trim(xml).toString
  }
}
