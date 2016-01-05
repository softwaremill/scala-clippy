package com.softwaremill.clippy

import org.scalajs.jquery._
import autowire._
import scala.concurrent.ExecutionContext.Implicits.global

object Contribute {
  private def readTemplate(id: String) = {
    val h = jQuery(s"#$id").html()
    jQuery(s"#$id").remove()
    h
  }

  private lazy val step2cannotparseTemplate = readTemplate("step2cannotparse_template")

  def setup(): Unit = {
    jQuery("#step1Next").click { (ev: JQueryEventObject) =>
      resetErrors()

      val errorText = jQuery("#errortext").value().asInstanceOf[String]
      jQuery("#step1").hide()
      CompilationErrorParser.parse(errorText) match {
        case None =>
          showStep2CannotParse(errorText)

        case Some(ce) =>
          jQuery("#step2parsed").show()
      }

      false
    }
  }

  private def showStep2CannotParse(errorText: String): Unit = {
    jQuery("#step2cannotparse").html(step2cannotparseTemplate).show()
    jQuery("#step2cannotparseReset").click { (ev: JQueryEventObject) => reset() }
    jQuery("#step2cannotparseSend").click { (ev: JQueryEventObject) =>
      resetErrors()

      requiredValueOrShowError("step2cannotparseEmail", "Email").foreach { email =>
        AutowireClient[ContributeApi].sendCannotParse(errorText, email).call().onSuccess {
          case _ =>
            reset()
            showSuccessMessage("Error submitted successfully! We'll get in touch soon.")
        }
      }

      false
    }
  }

  private def requiredValueOrShowError(id: String, name: String): Option[String] = {
    val v = jQuery(s"#$id").value().asInstanceOf[String]
    if (v == null || v.isEmpty) {
      showErrorMessage(s"$name is required")
      jQuery(s"#$id").parent(".form-group").addClass("has-error")
      None
    }
    else Some(v)
  }

  private def resetErrors(): Unit = {
    hideMessages()

    jQuery(".has-error").removeClass("has-error")
  }

  private def reset(): Unit = {
    hideMessages()

    jQuery("#step1").show()
    jQuery("#errortext").value("")

    jQuery("#step2cannotparse").html("").hide()
    jQuery("#step2parsed").html("").hide()
  }

  private def showSuccessMessage(msg: String): Unit = {
    jQuery("#contributeSuccessMessage").html(msg).show()
  }

  private def showErrorMessage(msg: String): Unit = {
    jQuery("#contributeErrorMessage").html(msg).show()
  }

  private def hideMessages(): Unit = {
    jQuery("#contributeSuccessMessage").html("").hide()
    jQuery("#contributeErrorMessage").html("").hide()
  }
}
