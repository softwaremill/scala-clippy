package util.email

import scala.concurrent.Future

trait EmailService {
  def send(to: String, subject: String, body: String): Future[Unit]
}
