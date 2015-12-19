package org.softwaremill.clippy

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{PluginComponent, Plugin}

class ClippyPlugin(val global: Global) extends Plugin {

  override val name: String = "clippy"

  override val components: List[PluginComponent] = Nil

  override val description: String = "gives good advice"
}
