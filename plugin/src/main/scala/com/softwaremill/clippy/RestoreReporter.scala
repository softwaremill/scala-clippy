package com.softwaremill.clippy

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

/**
  * Replaces global reporter back with the original global reporter. Sbt uses its own xsbt.DelegatingReporter
  * which we cannot replace outside of Scala compilation phases. This component makes sure that before the compilation
  * is over, original reporter gets reassigned to the global field.
  */
class RestoreReporter(val global: Global) extends PluginComponent {

  val originalReporter = global.reporter

  override val runsAfter = List[String]("jvm")
  override val runsBefore = List[String]("terminal")
  override val phaseName = "restore-original-reporter"

  override def newPhase(prev: Phase) = new Phase(prev) {

    override def name = phaseName

    override def description = "Switches the reporter from Clippy's DelegatingReporter back to original one"

    override def run(): Unit = {
      global.reporter = originalReporter
    }
  }

}
