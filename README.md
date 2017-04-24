# Scala clippy

[![Join the chat at https://gitter.im/softwaremill/scala-clippy](https://badges.gitter.im/softwaremill/scala-clippy.svg)](https://gitter.im/softwaremill/scala-clippy?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/softwaremill/scala-clippy.svg?branch=master)](https://travis-ci.org/softwaremill/scala-clippy)
[![Dependencies](https://app.updateimpact.com/badge/634276070333485056/clippy.svg?config=compile)](https://app.updateimpact.com/latest/634276070333485056/clippy)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.softwaremill.clippy/plugin_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.softwaremill.clippy/plugin_2.11)

Enrich your Scala compiler error output with additional advices and colors!

-![enriched error example](ui/app/assets/img/clippy-akka-err-rich.png "")

# Documentation

Read the detailed [documentation](https://scala-clippy.org)

# Contributing to the project

You can also help developing the plugin and/or the UI for submitting new advices! The module structure is:

* `model` - code shared between the UI and the plugin. Contains basic model case classes, such as `CompilationError` + parser
* `plugin` - the compiler plugin which actually displays the advices and matches errors agains the database of known errors
* `tests` - tests for the compiler plugin. Must be a separate project, as it requires the plugin jar to be ready
* `ui` - the ui server project in Play
* `ui-client` - the Scala.JS client-side code
* `ui-shared` - code shared between the UI server and UI client (but not needed for the plugin)

For examples on how to write tests for advice to ensure it does not go out of date see [CompileTests.scala](./tests/src/test/scala/org/softwaremill/clippy/CompileTests.scala).
If you want to write your own tests with compilation using `mkToolbox`, remember to add a `-P:clippy:testmode=true`
compiler option. It ensures that a correct reporter replacement mechanism is used, which needs to be different
specifically for tests. See [CompileTests.scala](tests/src/test/scala/org/softwaremill/clippy/CompileTests.scala) for reference.


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
