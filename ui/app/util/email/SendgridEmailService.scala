package util.email

import com.sendgrid.SendGrid
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future
import scala.util.Properties

class SendgridEmailService(sendgridUsername: String, sendgridPassword: String, emailFrom: String)
    extends EmailService
    with StrictLogging {

  private lazy val sendgrid = new SendGrid(sendgridUsername, sendgridPassword)

  override def send(to: String, subject: String, body: String) = {
    val email = new SendGrid.Email()
    email.addTo(to)
    email.setFrom(emailFrom)
    email.setSubject(subject)
    email.setText(body)

    val response = sendgrid.send(email)
    if (response.getStatus) {
      logger.info(s"Email to $to sent")
    } else {
      logger.error(
        s"Email to $to, subject: $subject, body: $body, not sent: " +
          s"${response.getCode}/${response.getMessage}"
      )
    }

    Future.successful(())
  }
}

object SendgridEmailService extends StrictLogging {
  def createFromEnv(emailFrom: String): Option[SendgridEmailService] =
    for {
      u <- Properties.envOrNone("SENDGRID_USERNAME")
      p <- Properties.envOrNone("SENDGRID_PASSWORD")
    } yield {
      logger.info("Using SendGrid email service")
      new SendgridEmailService(u, p, emailFrom)
    }
}
