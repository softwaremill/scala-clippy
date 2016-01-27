package com.softwaremill.clippy

import scala.reflect.internal.util.Position
import scala.tools.nsc.reporters.Reporter

class DelegatingReporter(r: Reporter, handleError: (Position, String) => String) extends Reporter {
  override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) = ???

  override def echo(msg: String) = r.echo(msg)
  override def comment(pos: Position, msg: String) = r.comment(pos, msg)
  override def hasErrors = r.hasErrors || cancelled
  override def reset() = {
    cancelled = false
    r.reset()
  }

  //

  override def echo(pos: Position, msg: String) = r.echo(pos, msg)
  override def warning(pos: Position, msg: String) = r.warning(pos, msg)
  override def errorCount = r.errorCount
  override def warningCount = r.warningCount
  override def hasWarnings = r.hasWarnings
  override def flush() = r.flush()
  override def count(severity: Severity): Int = r.count(conv(severity))
  override def resetCount(severity: Severity): Unit = r.resetCount(conv(severity))

  //

  private def conv(s: Severity): r.Severity = s match {
    case INFO => r.INFO
    case WARNING => r.WARNING
    case ERROR => r.ERROR
  }

  //

  override def error(pos: Position, msg: String) = r.error(pos, handleError(pos, msg))
}
