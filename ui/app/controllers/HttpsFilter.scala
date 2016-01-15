package controllers

import play.api.http.Status
import play.api.mvc.{Result, Results, Filter, RequestHeader}

import scala.concurrent.Future

class HttpsFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    requestHeader.headers.get("x-forwarded-proto") match {
      case Some(header) =>
        if (header == "https") {
          nextFilter(requestHeader)
        }
        else {
          Future.successful(Results.Redirect("https://" + requestHeader.host + requestHeader.uri, Status.MOVED_PERMANENTLY))
        }
      case None => nextFilter(requestHeader)
    }
  }
}
