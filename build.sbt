import sbt._
import Keys._

import scalariform.formatter.preferences._

// testing
val scalatest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"
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
  .settings(
    publishArtifact := false,
    // heroku
    herokuFatJar in Compile := Some((assemblyOutputPath in ui in assembly).value),
    deployHeroku in Compile <<= (deployHeroku in Compile) dependsOn (assembly in ui)
  )
  .aggregate(modelJvm, plugin, tests, ui)

lazy val model = (crossProject.crossType(CrossType.Pure) in file("model"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(scalatest, scalacheck, "org.scala-lang.modules" %% "scala-xml" % "1.0.5")
  )

lazy val modelJvm = model.jvm.settings(name := "modelJvm")
lazy val modelJs = model.js.settings(name := "modelJs")

lazy val plugin = (project in file("plugin"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      scalatest, scalacheck),
    buildInfoPackage := "com.softwaremill.clippy",
    buildInfoObject := "ClippyBuildInfo",
    // including the model classes for re-compilation, so that the plugin jar has no deps
    unmanagedSourceDirectories in Compile ++= (sourceDirectories in (modelJvm, Compile)).value
  )

lazy val pluginJar = Keys.`package` in (plugin, Compile)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      scalatest,
      "com.typesafe.akka" %% "akka-http-experimental" % "2.0.1",
      "com.softwaremill.macwire" %% "macros" % "2.2.2" % "provided"
    ),
    scalacOptions += s"-Xplugin:${pluginJar.value.getAbsolutePath}",
    envVars in Test := (envVars in Test).value + ("CLIPPY_PLUGIN_PATH" -> pluginJar.value.getAbsolutePath),
    fork in Test := true
  ).dependsOn(plugin)

val slickVersion = "3.1.1"

lazy val ui: Project = (project in file("ui"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.h2database" % "h2" % "1.4.190", // % "test",
      scalatest,
      "org.webjars" %% "webjars-play" % "2.4.0-1",
      "org.webjars" % "bootstrap" % "3.3.6",
      "org.webjars" % "jquery" % "1.11.3",
      "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
      "com.softwaremill.common" %% "id-generator" % "1.1.0",
      "com.sendgrid" % "sendgrid-java" % "2.2.2" exclude("commons-logging", "commons-logging"),
      "org.postgresql" % "postgresql" % "9.4.1207",
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
      "org.flywaydb" % "flyway-core" % "3.2.1"
    ),
    scalaJSProjects := Seq(uiClient),
    pipelineStages := Seq(scalaJSProd),
    routesGenerator := InjectedRoutesGenerator,
    // heroku & fat-jar
    assemblyJarName in assembly := "app.jar",
    mainClass in assembly := Some("play.core.server.ProdServerStart"),
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    buildInfoPackage := "util",
    buildInfoObject := "ClippyBuildInfo",
    assemblyMergeStrategy in assembly := {
      // anything in public/lib is copied from webjars and causes duplicate resources exceptions
      case PathList("public", "lib", xs @ _*) => MergeStrategy.discard
      case "JS_DEPENDENCIES" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
  .enablePlugins(PlayScala)
  .aggregate(uiClient)
  .dependsOn(uiSharedJvm)

val scalaJsReactVersion = "0.10.3"

lazy val uiClient: Project = (project in file("ui-client"))
  .settings(commonSettings)
  .settings(name := "uiClient")
  .settings(
    persistLauncher := true,
    persistLauncher in Test := false,
    addCompilerPlugin(compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)), // for @Lenses
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.2",
      "be.doeraene" %%% "scalajs-jquery" % "0.8.1",
      "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReactVersion,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle" % scalaJsReactVersion,
      "com.github.japgolly.fork.monocle" %%% "monocle-macro" % "1.2.0"
    ),
    jsDependencies ++= Seq(
      RuntimeDOM % "test",
      "org.webjars.bower" % "react" % "0.14.3" / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
      "org.webjars.bower" % "react" % "0.14.3" / "react-dom.js" minified  "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
    )
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
  .dependsOn(model)

lazy val uiSharedJvm = uiShared.jvm.settings(name := "uiSharedJvm")
lazy val uiSharedJs = uiShared.js.settings(name := "uiSharedJs")