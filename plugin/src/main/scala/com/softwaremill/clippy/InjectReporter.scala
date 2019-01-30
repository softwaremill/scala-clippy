package com.softwaremill.clippy

import scala.reflect.internal.util.Position
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

/**
  * Responsible for replacing the global reporter with our custom Clippy reporter after the first phase of compilation.
  */
abstract class InjectReporter(
    handleError: (Position, String) => String,
    getFatalWarningAdvice: String => Option[Warning],
    superGlobal: Global
) extends PluginComponent {

  override val global = superGlobal
  def colorsConfig: ColorsConfig
  def isEnabled: Boolean
  override val runsAfter  = List[String]("parser")
  override val runsBefore = List[String]("namer")
  override val phaseName  = "inject-clippy-reporter"

  override def newPhase(prev: Phase) = new Phase(prev) {

    override def name = phaseName

    override def description = "Switches the reporter to Clippy's reporter chain"

    override def run(): Unit =
      if (isEnabled) {
        val r = global.reporter
        global.reporter = new FailOnWarningsReporter(
          new DelegatingReporter(r, handleError, colorsConfig),
          getFatalWarningAdvice,
          colorsConfig
        )
      }
  }

}
