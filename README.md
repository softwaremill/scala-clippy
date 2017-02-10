# Scala clippy

[![Join the chat at https://gitter.im/softwaremill/scala-clippy](https://badges.gitter.im/softwaremill/scala-clippy.svg)](https://gitter.im/softwaremill/scala-clippy?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/softwaremill/scala-clippy.svg?branch=master)](https://travis-ci.org/softwaremill/scala-clippy)
[![Dependencies](https://app.updateimpact.com/badge/634276070333485056/clippy.svg?config=compile)](https://app.updateimpact.com/latest/634276070333485056/clippy)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.softwaremill.clippy/plugin_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.softwaremill.clippy/plugin_2.11)

Did you ever see a Scala compiler error such as:

````scala
[error] TheNextFacebook.scala:16: type mismatch;
[error]  found   : akka.http.scaladsl.server.StandardRoute
[error]  required: akka.stream.scaladsl.Flow[akka.http.scaladsl.model.HttpRequest,akka.http.scaladsl.model.HttpResponse,Any]
[error]   Http().bindAndHandle(r, "localhost", 8080)
````

and had no idea what to do next? Well in this case, you need to provide an implicit instance of an `ActorMaterializer`,
but the compiler isn't smart enough to be able to tell you that. Luckily, **ScalaClippy is here to help**!

Just add the compiler plugin, and you'll see this additional helpful message:

````scala
[error]  Clippy advises: did you forget to define an implicit akka.stream.ActorMaterializer?
[error]  It allows routes to be converted into a flow.
[error]  You can read more at http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0/scala/http/routing-dsl/index.html
````

# Adding the plugin

The easiest to use Clippy is via an SBT plugin. If you'd like Clippy to be enabled for all projects, without
the need to modify each project's build, add the following to `~/.sbt/0.13/plugins/build.sbt`:

````scala
addSbtPlugin("com.softwaremill.clippy" % "plugin-sbt" % "0.5.0")
````

Upon first use, the plugin will download the advice dataset from `https://scala-clippy.org` and store it in the
`$HOME/.clippy` directory. The dataset will be updated at most once a day, in the background. You can customize the
dataset URL and local store by setting the `clippyUrl` and `clippyLocalStoreDir` sbt options to non-`None`-values.

Note: to customize a global sbt plugin (a plugin which is added via `~/.sbt/0.13/plugins/build.sbt`) keep in mind
that:

* customize the plugin settings in `~/.sbt/0.13/build.sbt` (one directory up!). These settings will be
automatically added to all of your projects.
* you'll need to add `import com.softwaremill.clippy.ClippySbtPlugin._` to access the setting names as auto-imports 
don't work in the global settings
* the clippy sbt settings are just a convenient syntax for adding compiler options (e.g., enabling colors is same 
as `scalacOptions += "-P:clippy:colors=true"`)

# Enabling syntax and type mismatch diffs highlighting

Clippy can highlight:

* in type mismatch errors, the diff between expected and actual types. This may be especially helpful for long type
signatures
* syntax when displaying code fragments with errors

If you'd like to enable this feature in sbt globally, add the following to `~/.sbt/0.13/build.sbt`: (see also notes
above)
 
````scala
import com.softwaremill.clippy.ClippySbtPlugin._ // needed in global configuration only
clippyColorsEnabled := true
````

To customize the colors, set any of `clippyColorDiff`, `clippyColorComment`,
`clippyColorType`, `clippyColorLiteral`, `clippyColorKeyword`, `clippyColorReset` to `Some(ClippyColor.[name])`, where `[name]` can be:
`Black`, `Red`, `Green`, `Yellow`, `Blue`, `Magenta`, `Cyan`, `White`, `LightGray` or `None`.

### Windows Users
If you notice that colors don't get correctly reset, that's probably caused by problems with interpreting the
standard ANSI Reset code. You can set `clippyColorReset` to a custom value like `LightGray` to solve this issue.

You can of course add Clippy on a per-project basis as well.

# Contributing advice

Scala Clippy is only as good as its advice database. Help other users by submitting a fix for a compilation error that
you have encountered!

It will only take you a couple of minutes, no registration required. Just head over to
[https://scala-clippy.org](https://scala-clippy.org)! Thanks!

# Project specific advice

If you have advice that you feel is too specific to be worth sharing on [https://scala-clippy.org](https://scala-clippy.org)
you can add it to your project specific advice file.
First set your project root:

````
clippyProjectRoot := Some((baseDirectory in ThisBuild).value) 
````

Then create a file named .clippy.json in the root of your project directory and add the advice json in the format illustrated below:

````
{
  "advices": [
    {
      "error": {
        "type": "typeMismatch",
        "found": "scala\\.concurrent\\.Future\\[Int\\]",
        "required": "Int"
      },
      "text": "Maybe you used map where you should have used flatMap?",
      "library": {
        "groupId": "scala.lang",
        "artifactId": "Future",
        "version": "1.0"
      }
    }
  ]
}
````

# Library specific advice

If you have advice that is specific to a library or library version you can also bundle the advice with your library.
If your users have Scala-Clippy installed they will see your advice if your library is inclued in their project.
This can be helpful in the common case where users of your library need specific imports to be able to use your functionality.
To bundle clippy advice with your library just put it in a file named clippy.json in your resources directory

For examples on how to write tests for advice to ensure it does not go out of date see [CompileTests.scala](./tests/src/test/scala/org/softwaremill/clippy/CompileTests.scala)

# Alternative ways to use Clippy

You can also use Clippy directly as a compiler plugin. If you use SBT, add the following setting to your
project's `.sbt` file:

````scala
addCompilerPlugin("com.softwaremill.clippy" %% "plugin" % "0.5.0" classifier "bundle")
````

If you are using `scalac` directly, add the following option:

````scala
-Xplugin:clippy-plugin_2.11-0.5.0-bundle.jar
````

This can be followed by any of the available options, e.g. `-P:clippy:colors=true`.

If you would like local advice to work in intellij which defaults to running in a different directory than the project:

````scala
-P:clippy:projectRoot=$projectDir
````

# Contributing to the project

You can also help developing the plugin and/or the UI for submitting new advices! The module structure is:

* `model` - code shared between the UI and the plugin. Contains basic model case classes, such as `CompilationError` + parser
* `plugin` - the compiler plugin which actually displays the advices and matches errors agains the database of known errors
* `tests` - tests for the compiler plugin. Must be a separate project, as it requires the plugin jar to be ready
* `ui` - the ui server project in Play
* `ui-client` - the Scala.JS client-side code
* `ui-shared` - code shared between the UI server and UI client (but not needed for the plugin)

If you want to write your own tests with compilation using `mkToolbox`, remember to add a `-P:clippy:testmode=true`
compiler option. It ensures that a correct reporter replacement mechanism is used, which needs to be different
specifically for tests. See [CompileTests.scala](tests/src/test/scala/org/softwaremill/clippy/CompileTests.scala) for
reference.

To publish locally append "-SNAPSHOT" to the version number then run
````scala
sbt "project plugin" "+ publishLocal"
````

Run advice tests with
````scala
sbt tests/test
````

# Heroku deployment

Locally:

* Install the Heroku Toolbelt
* link the local git repository with the Heroku application: `heroku git:remote -a scala-clippy`
* run `sbt deployHeroku` to deploy the current code as a fat-jar

Currently deployed on `https://www.scala-clippy.org`

# Credits

Clippy contributors:

* [Krzysztof Ciesielski](https://github.com/kciesielski)
* [Łukasz Żuchowski](https://github.com/Zuchos)
* [Shane Delmore](https://github.com/ShaneDelmore)
* [Adam Warski](https://github.com/adamw)

Syntax highlighting code is copied from [Ammonite](http://www.lihaoyi.com/Ammonite/).
