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
  private lazy val step2parsedTemplate = readTemplate("step2parsed_template")

  def setup(): Unit = {
    jQuery("#step1Next").click { (ev: JQueryEventObject) =>
      resetErrors()

      val errorText = jQuery("#errortext").value().asInstanceOf[String]
      jQuery("#step1").hide()
      CompilationErrorParser.parse(errorText) match {
        case None => showStep2CannotParse(errorText)
        case Some(ce) => showStep2Parsed(ce)
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

  private def showStep2Parsed(ce: CompilationError): Unit = {
    jQuery("#step2parsed").html(step2parsedTemplate).show()
    jQuery("#step2parsedInfo").html(ce.toString)
    jQuery("#step2parsedReset").click { (ev: JQueryEventObject) => reset() }
    jQuery("#step2parsedSend").click { (ev: JQueryEventObject) =>
      resetErrors()

      for {
        advice <- requiredValueOrShowError("step2advice", "Advice text")
        libraryGroupId <- requiredValueOrShowError("step2GroupId", "Library group id")
        libraryArtifactId <- requiredValueOrShowError("step2ArtifactId", "Library artifact id")
        libraryVersion <- requiredValueOrShowError("step2Version", "Library version")
        email = optionalValue("step2parsedEmail")
        twitter = optionalValue("step2parsedTwitter")
        github = optionalValue("step2parsedGithub")
        comment = optionalValue("step2comment")
      } {
        AutowireClient[ContributeApi].sendAdviceProposal(ce, advice,
          Library(libraryGroupId, libraryArtifactId, libraryVersion),
          Contributor(email, twitter, github), comment)
          .call().onSuccess {
            case _ =>
              reset()
              showSuccessMessage("Advice submitted successfully! We'll get in touch soon, and let you know when your proposal is accepted.")
          }
      }

      false
    }
  }

  private def optionalValue(id: String): Option[String] = {
    val v = jQuery(s"#$id").value().asInstanceOf[String]
    if (v == null || v.trim().isEmpty) None else Some(v.trim())
  }

  private def requiredValueOrShowError(id: String, name: String): Option[String] = {
    optionalValue(id) match {
      case None =>
        showErrorMessage(s"$name is required")
        jQuery(s"#$id").parent(".form-group").addClass("has-error")
        None

      case x => x
    }
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
