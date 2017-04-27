package com.softwaremill.clippy

import scala.reflect.internal.util.Position
import scala.tools.nsc.reporters.Reporter

class DelegatingReporter(r: Reporter, handleError: (Position, String) => String, colorsConfig: ColorsConfig)
    extends Reporter {
  override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) = {
    val wrapped = DelegatingPosition.wrap(pos, colorsConfig)

    // cannot delegate to info0 as it's protected, hence special-casing on the possible severity values
    if (severity == INFO) {
      r.info(wrapped, msg, force)
    } else if (severity == WARNING) {
      warning(wrapped, msg)
    } else if (severity == ERROR) {
      error(wrapped, msg)
    } else {
      error(wrapped, s"UNKNOWN SEVERITY: $severity\n$msg")
    }
  }

  override def echo(msg: String)                   = r.echo(msg)
  override def comment(pos: Position, msg: String) = r.comment(DelegatingPosition.wrap(pos, colorsConfig), msg)
  override def hasErrors                           = r.hasErrors || cancelled
  override def reset() = {
    cancelled = false
    r.reset()
  }

  //

  override def echo(pos: Position, msg: String)     = r.echo(DelegatingPosition.wrap(pos, colorsConfig), msg)
  override def warning(pos: Position, msg: String)  = r.warning(DelegatingPosition.wrap(pos, colorsConfig), msg)
  override def errorCount                           = r.errorCount
  override def warningCount                         = r.warningCount
  override def hasWarnings                          = r.hasWarnings
  override def flush()                              = r.flush()
  override def count(severity: Severity): Int       = r.count(conv(severity))
  override def resetCount(severity: Severity): Unit = r.resetCount(conv(severity))

  //

  private def conv(s: Severity): r.Severity = s match {
    case INFO    => r.INFO
    case WARNING => r.WARNING
    case ERROR   => r.ERROR
  }

  //

  override def error(pos: Position, msg: String) = {
    val wrapped = DelegatingPosition.wrap(pos, colorsConfig)
    r.error(wrapped, handleError(wrapped, msg))
  }
}
