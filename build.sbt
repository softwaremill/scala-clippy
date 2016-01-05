import sbt._
import Keys._

import scalariform.formatter.preferences._

// testing
val scalatest = "org.scalatest" %% "scalatest" % "2.2.5" % "test"
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"

name := "clippy"

// factor out common settings into a sequence
lazy val commonSettings = scalariformSettings ++ Seq(
  organization := "com.softwaremill.clippy",
  version := "0.1",
  scalaVersion := "2.11.7",

  scalacOptions ++= Seq("-unchecked", "-deprecation"),

  parallelExecution := false,

  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(CompactControlReadability, true)
    .setPreference(SpacesAroundMultiImports, false),

  // Sonatype OSS deployment
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  credentials   += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <scm>
      <url>git@github.com:adamw/macwire.git</url>
      <connection>scm:git:git@github.com:adamw/macwire.git</connection>
    </scm>
      <developers>
        <developer>
          <id>adamw</id>
          <name>Adam Warski</name>
          <url>http://www.warski.org</url>
        </developer>
      </developers>,
  licenses      := ("Apache2", new java.net.URL("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil,
  homepage      := Some(new java.net.URL("http://www.softwaremill.com")),
  com.updateimpact.Plugin.apiKey in ThisBuild := sys.env.getOrElse("UPDATEIMPACT_API_KEY", (com.updateimpact.Plugin.apiKey in ThisBuild).value)
)

lazy val clippy = (project in file("."))
  .settings(commonSettings)
  .settings(publishArtifact := false)
  .aggregate(plugin, tests, ui)

lazy val plugin = (project in file("plugin"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      scalatest, scalacheck)
  )

lazy val pluginJar = Keys.`package` in (plugin, Compile)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      scalatest,
      "com.typesafe.akka" %% "akka-http-experimental" % "2.0",
      "com.softwaremill.macwire" %% "macros" % "2.2.2" % "provided"
    ),
    scalacOptions += s"-Xplugin:${pluginJar.value.getAbsolutePath}",
    envVars in Test := (envVars in Test).value + ("CLIPPY_PLUGIN_PATH" -> pluginJar.value.getAbsolutePath),
    fork in Test := true
  ) dependsOn (plugin)

lazy val ui: Project = (project in file("ui"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-slick" % "1.1.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "1.1.0",
      "com.h2database" % "h2" % "1.4.190", // % "test",
      scalatest,
      "org.webjars" %% "webjars-play" % "2.4.0-1",
      "org.webjars" % "bootstrap" % "3.3.6",
      "org.webjars" % "jquery" % "1.11.3",
      "com.vmunier" %% "play-scalajs-scripts" % "0.3.0"
    ),
    scalaJSProjects := Seq(uiClient),
    pipelineStages := Seq(scalaJSProd),
    routesGenerator := InjectedRoutesGenerator
  )
  .enablePlugins(PlayScala)
  .aggregate(uiClient)
  .dependsOn(uiSharedJvm)

lazy val uiClient: Project = (project in file("ui-client"))
  .settings(commonSettings)
  .settings(name := "uiClient")
  .settings(
    persistLauncher := true,
    persistLauncher in Test := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "be.doeraene" %%% "scalajs-jquery" % "0.8.1"
    ),
    jsDependencies += RuntimeDOM % "test"
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(uiSharedJs)

lazy val uiShared = (crossProject.crossType(CrossType.Pure) in file("ui-shared"))
  .settings(commonSettings: _*)
  .settings(
    name := "uiShared",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "autowire" % "0.2.5",
      "com.lihaoyi" %%% "upickle" % "0.3.6"
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val uiSharedJvm = uiShared.jvm.settings(name := "uiSharedJvm")
lazy val uiSharedJs = uiShared.js.settings(name := "uiSharedJs")