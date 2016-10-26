package com.softwaremill.clippy

import sbt._
import Keys._

object ClippySbtPlugin extends AutoPlugin {
  override lazy val projectSettings = Seq(
    addCompilerPlugin("com.softwaremill.clippy" %% "plugin" % ClippyBuildInfo.version classifier "bundle"),
    scalacOptions += "-P:clippy:projectRoot=" + (baseDirectory in ThisBuild).value
  )

  override def trigger = allRequirements
}
