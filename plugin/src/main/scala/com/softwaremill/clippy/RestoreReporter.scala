package com.softwaremill.clippy

import scala.reflect.internal.util.Position
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

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
