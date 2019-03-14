import sbt._
import Keys._
import sbtassembly.AssemblyKeys

import scala.xml.transform.RuleTransformer
import scala.xml.transform.RewriteRule
import scala.xml.{Node => XNode}
import scala.xml.{NodeSeq => XNodeSeq}
import scala.xml.{Elem => XElem}

val slickVersion = "3.1.1"

val json4s = "org.json4s" %% "json4s-native" % "3.5.0"

// testing
val scalatest  = "org.scalatest"  %% "scalatest"  % "3.0.1"  % "test"
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"

name := "clippy"

// factor out common settings into a sequence
lazy val commonSettingsNoScalaVersion = Seq(
  organization := "com.softwaremill.clippy",
  version := "0.6.1",
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  parallelExecution := false,
  // Sonatype OSS deployment
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  pomExtra :=
    <scm>
      <url>git@github.com:softwaremill/scala-clippy.git</url>
      <connection>scm:git:git@github.com:softwaremill/scala-clippy.git</connection>
    </scm>
      <developers>
        <developer>
          <id>adamw</id>
          <name>Adam Warski</name>
          <url>http://www.warski.org</url>
        </developer>
      </developers>,
  licenses := ("Apache2", new java.net.URL("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil,
  homepage := Some(new java.net.URL("http://www.softwaremill.com")),
  com.updateimpact.Plugin.apiKey in ThisBuild := sys.env
    .getOrElse("UPDATEIMPACT_API_KEY", (com.updateimpact.Plugin.apiKey in ThisBuild).value)
)

lazy val commonSettings = commonSettingsNoScalaVersion ++ Seq(
  scalaVersion := "2.11.11"
)

lazy val sbt10CompatSettings = Seq(
  sbtVersion in Global := (if (scalaVersion.value startsWith "2.12.") "1.1.6" else "0.13.15"),
  scalaCompilerBridgeSource := ("org.scala-sbt" % "compiler-interface" % "0.13.15" % "component").sources
)

lazy val clippy = (project in file("."))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    // heroku
    herokuFatJar in Compile := Some((assemblyOutputPath in ui in assembly).value),
    deployHeroku in Compile := (deployHeroku in Compile).dependsOn(assembly in ui).value
  )
  .aggregate(modelJvm, plugin, pluginSbt, tests, ui)

lazy val model = (crossProject.crossType(CrossType.Pure) in file("model"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(scalatest, scalacheck, json4s)
  )

lazy val modelJvm = model.jvm.settings(name := "modelJvm")
lazy val modelJs  = model.js.settings(name := "modelJs")

def removeDep(groupId: String, artifactId: String) = new RewriteRule {
  override def transform(n: XNode): XNodeSeq = n match {
    case e: XElem if (e \ "groupId").text == groupId && (e \ "artifactId").text.startsWith(artifactId) =>
      XNodeSeq.Empty
    case _ => n
  }
}

lazy val plugin = (project in file("plugin"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value, "2.12.1", "2.10.6"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "com.lihaoyi"    %% "scalaparse"    % "0.4.2",
      "com.lihaoyi"    %% "fansi"         % "0.2.3",
      scalatest,
      scalacheck,
      json4s
    ),
    // this is needed for fastparse to work on 2.10
    libraryDependencies ++= (if (scalaVersion.value startsWith "2.10.")
                               Seq(compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full))
                             else Seq()),
    pomPostProcess := { (node: XNode) =>
      new RuleTransformer(removeDep("org.json4s", "json4s-native")).transform(node).head
    },
    buildInfoPackage := "com.softwaremill.clippy",
    buildInfoObject := "ClippyBuildInfo",
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("bundle"))
    },
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    // including the model classes for re-compilation, as for some reason depending on modelJvm doesn't work
    unmanagedSourceDirectories in Compile ++= (sourceDirectories in (modelJvm, Compile)).value
  )
  .settings(sbt10CompatSettings)
  .settings(addArtifact(artifact in (Compile, assembly), assembly))

lazy val pluginJar = AssemblyKeys.`assembly` in (plugin, Compile)

lazy val pluginSbt = (project in file("plugin-sbt"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettingsNoScalaVersion)
  .settings(
    sbtPlugin := true,
    name := "plugin-sbt",
    buildInfoPackage := "com.softwaremill.clippy",
    buildInfoObject := "ClippyBuildInfo",
    scalaVersion := "2.10.6",
    crossSbtVersions := Vector("0.13.16", "1.0.0")
  )
  .settings(sbt10CompatSettings)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      json4s,
      scalatest,
      "com.typesafe.akka"        %% "akka-http" % "10.0.0",
      "com.softwaremill.macwire" %% "macros"    % "2.2.2" % "provided",
      "com.typesafe.slick"       %% "slick"     % slickVersion
    ),
    // during tests, read from the local repository, if at all available
    scalacOptions ++= List(
      s"-Xplugin:${pluginJar.value.getAbsolutePath}",
      "-P:clippy:url=http://localhost:9000",
      "-P:clippy:colors=true"
    ),
    envVars in Test := (envVars in Test).value + ("CLIPPY_PLUGIN_PATH" -> pluginJar.value.getAbsolutePath),
    fork in Test := true
  )
  .dependsOn(modelJvm)

lazy val ui: Project = (project in file("ui"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.h2database" % "h2" % "1.4.190", // % "test",
      scalatest,
      "org.webjars"             %% "webjars-play"         % "2.4.0-1",
      "org.webjars"             % "bootstrap"             % "3.3.6",
      "org.webjars"             % "jquery"                % "1.11.3",
      "com.vmunier"             %% "play-scalajs-scripts" % "0.3.0",
      "com.softwaremill.common" %% "id-generator"         % "1.1.0",
      "com.sendgrid"            % "sendgrid-java"         % "2.2.2" exclude ("commons-logging", "commons-logging"),
      "org.postgresql"          % "postgresql"            % "9.4.1207",
      "com.typesafe.slick"      %% "slick"                % slickVersion,
      "com.typesafe.slick"      %% "slick-hikaricp"       % slickVersion,
      "org.flywaydb"            % "flyway-core"           % "3.2.1"
    ),
    scalaJSProjects := Seq(uiClient),
    pipelineStages in Assets := Seq(scalaJSProd),
    routesGenerator := InjectedRoutesGenerator,
    // heroku & fat-jar
    assemblyJarName in assembly := "app.jar",
    mainClass in assembly := Some("play.core.server.ProdServerStart"),
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    buildInfoPackage := "util",
    buildInfoObject := "ClippyBuildInfo",
    assemblyMergeStrategy in assembly := {
      // anything in public/lib is copied from webjars and causes duplicate resources exceptions
      case PathList("public", "lib", xs @ _ *) => MergeStrategy.discard
      case "JS_DEPENDENCIES"                   => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
  .enablePlugins(PlayScala)
  .aggregate(uiClient)
  .dependsOn(uiSharedJvm)

val scalaJsReactVersion = "0.11.3"

lazy val uiClient: Project = (project in file("ui-client"))
  .settings(commonSettings)
  .settings(name := "uiClient")
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false,
    addCompilerPlugin(compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)), // for @Lenses
    libraryDependencies ++= Seq(
      "org.scala-js"                      %%% "scalajs-dom"    % "0.9.1",
      "be.doeraene"                       %%% "scalajs-jquery" % "0.9.1",
      "com.github.japgolly.scalajs-react" %%% "core"           % scalaJsReactVersion,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle"    % scalaJsReactVersion,
      "com.github.japgolly.fork.monocle"  %%% "monocle-macro"  % "1.2.0"
    ),
    jsDependencies ++= Seq(
      RuntimeDOM          % "test",
      "org.webjars.bower" % "react" % "15.3.2" / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
      "org.webjars.bower" % "react" % "15.3.2" / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(uiSharedJs)

lazy val uiShared = (crossProject.crossType(CrossType.Pure) in file("ui-shared"))
  .settings(commonSettings: _*)
  .settings(
    name := "uiShared",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "autowire" % "0.2.5",
      "com.lihaoyi" %%% "upickle"  % "0.3.6"
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSWeb)
  .dependsOn(model)

lazy val uiSharedJvm = uiShared.jvm.settings(name := "uiSharedJvm")
lazy val uiSharedJs  = uiShared.js.settings(name := "uiSharedJs")
