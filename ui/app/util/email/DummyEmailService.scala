package util.email

import com.typesafe.scalalogging.StrictLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class DummyEmailService extends EmailService with StrictLogging {
  private val sentEmails: ListBuffer[(String, String, String)] = ListBuffer()

  logger.info("Using dummy email service")

  def reset() {
    sentEmails.clear()
  }

  override def send(to: String, subject: String, body: String) = {
    this.synchronized {
      sentEmails.+=((to, subject, body))
    }

    logger.info(s"Would send email to $to, with subject: $subject, body: $body")
    Future.successful(())
  }

  def wasEmailSent(to: String, subject: String): Boolean =
    sentEmails.exists(email => email._1.contains(to) && email._2 == subject)
}
