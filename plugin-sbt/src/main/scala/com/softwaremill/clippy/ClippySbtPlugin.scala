package com.softwaremill.clippy

import sbt._
import sbt.Keys._

import scala.collection.mutable.ListBuffer

object ClippySbtPlugin extends AutoPlugin {
  object ClippyColor extends Enumeration {
    val Black = Value("black")
    val LightGray = Value("light-gray")
    val DarkGray = Value("dark-gray")
    val Red = Value("red")
    val LightRed = Value("light-red")
    val Green = Value("green")
    val LightGreen = Value("light-green")
    val Yellow = Value("yellow")
    val LightYellow = Value("light-yellow")
    val Blue = Value("blue")
    val LightBlue = Value("light-blue")
    val Magenta = Value("magenta")
    val LightMagenta = Value("light-magenta")
    val Cyan = Value("cyan")
    val LightCyan = Value("light-cyan")
    val White = Value("white")
    val None = Value("none")
  }

  object autoImport {
    val clippyColorsEnabled = settingKey[Boolean]("Should Clippy color type mismatch diffs and highlight syntax")
    val clippyColorDiff = settingKey[Option[ClippyColor.Value]]("The color to use for diffs, if other than default")
    val clippyColorComment = settingKey[Option[ClippyColor.Value]]("The color to use for comments, if other than default")
    val clippyColorType = settingKey[Option[ClippyColor.Value]]("The color to use for types, if other than default")
    val clippyColorLiteral = settingKey[Option[ClippyColor.Value]]("The color to use for literals, if other than default")
    val clippyColorKeyword = settingKey[Option[ClippyColor.Value]]("The color to use for keywords, if other than default")
    val clippyColorReset = settingKey[Option[ClippyColor.Value]]("The color to use for resetting to neutral, if other than default")
    val clippyUrl = settingKey[Option[String]]("Url from which to fetch advice, if other than default")
    val clippyLocalStoreDir = settingKey[Option[String]]("Directory where cached advice data should be stored, if other than default")
    val clippyProjectRoot = settingKey[Option[String]]("Project root in which project-specific advice is stored, if any")
  }

  // in ~/.sbt auto import doesn't work, so providing aliases here for convenience
  val clippyColorsEnabled = autoImport.clippyColorsEnabled
  val clippyColorDiff = autoImport.clippyColorDiff
  val clippyColorComment = autoImport.clippyColorComment
  val clippyColorType = autoImport.clippyColorType
  val clippyColorLiteral = autoImport.clippyColorLiteral
  val clippyColorKeyword = autoImport.clippyColorKeyword
  val clippyColorReset = autoImport.clippyColorReset
  val clippyUrl = autoImport.clippyUrl
  val clippyLocalStoreDir = autoImport.clippyLocalStoreDir
  val clippyProjectRoot = autoImport.clippyProjectRoot

  override def projectSettings = Seq(
    clippyColorsEnabled := false,
    clippyColorDiff := None,
    clippyColorComment := None,
    clippyColorType := None,
    clippyColorLiteral := None,
    clippyColorKeyword := None,
    clippyColorReset := None,
    clippyUrl := None,
    clippyLocalStoreDir := None,
    clippyProjectRoot := None,

    addCompilerPlugin("com.softwaremill.clippy" %% "plugin" % ClippyBuildInfo.version classifier "bundle"),

    scalacOptions := {
      val result = ListBuffer(scalacOptions.value: _*)
      if (clippyColorsEnabled.value) result += "-P:clippy:colors=true"
      clippyColorDiff.value.foreach(c => result += s"-P:clippy:colors-diff=$c")
      clippyColorComment.value.foreach(c => result += s"-P:clippy:colors-comment=$c")
      clippyColorType.value.foreach(c => result += s"-P:clippy:colors-type=$c")
      clippyColorLiteral.value.foreach(c => result += s"-P:clippy:colors-literal=$c")
      clippyColorKeyword.value.foreach(c => result += s"-P:clippy:colors-keyword=$c")
      clippyColorReset.value.foreach(c => result += s"-P:clippy:colors-reset=$c")
      clippyUrl.value.foreach(c => result += s"-P:clippy:url=$c")
      clippyLocalStoreDir.value.foreach(c => result += s"-P:clippy:store=$c")
      clippyProjectRoot.value.foreach(c => result += s"-P:clippy:projectRoot=$c")
      result.toList
    }
  )

  override def trigger = allRequirements
}
