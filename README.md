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
`````

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
the need to modify their build, add the following to `~/.sbt/0.13/plugins/build.sbt`:

````scala
addSbtPlugin("com.softwaremill.clippy" % "plugin-sbt" % "0.2")
````

Upon first use, the plugin will download the advice dataset from `https://scala-clippy.org` and store it in the 
`$HOME/.clippy` directory. The dataset will be updated at most once a day, in the background. You can customize the
dataset URL and local store by using the `-P:clippy:url=` and `-P:clippy:store=` compiler options. 

# Contributing advice

Scala Clippy is only as good as its advice database. Help other users by submitting a fix for a compilation error that 
you have encountered!

It will only take you a couple of minutes, no registration required. Just head over to 
[https://scala-clippy.org](https://scala-clippy.org)! Thanks!

# Alternative ways to use Clippy

You can also use Clippy directly as a compiler plugin. If you use SBT, add the following setting to your
project's `.sbt` file:

````scala
addCompilerPlugin("com.softwaremill.clippy" %% "plugin" % "0.2" classifier "bundle")
````

If you are using `scalac` directly, add the following option:

````scala
-Xplugin:clippy-plugin_2.11-0.2-bundle.jar
````

# Contributing to the project

You can also help developing the plugin and/or the UI for submitting new advices! The module structure is:

* `model` - code shared between the UI and the plugin. Contains basic model case classes, such as `CompilationError` + parser
* `plugin` - the compiler plugin which actually displays the advices and matches errors agains the database of known errors
* `tests` - tests for the compiler plugin. Must be a separate project, as it requires the plugin jar to be ready
* `ui` - the ui server project in Play
* `ui-client` - the Scala.JS client-side code
* `ui-shared` - code shared between the UI server and UI client (but not needed for the plugin)

# Heroku deployment

Locally:

* Install the Heroku Toolbelt
* link the local git repository with the Heroku application: `heroku git:remote -a scala-clippy`
* run `sbt deployHeroku` to deploy the current code as a fat-jar

Currently deployed on `https://www.scala-clippy.org`
