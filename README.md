# Scala clippy

[![Build Status](https://travis-ci.org/softwaremill/scala-clippy.svg?branch=master)](https://travis-ci.org/softwaremill/scala-clippy)

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
````

# Adding the plugin

In your SBT build file, add:

````scala
addCompilerPlugin("com.softwaremill.clippy" % "plugin" % "0.1" cross CrossVersion.full)
````

# Library authors

If you'd like to provide custom advices for your library, just include a `clippy.xml` file in your library's jar,
it will be automatically picked by the compiler. Currently type mismatch and not found errors are supported. You can
also use regular expressions to match advices. Examples:

````xml
<clippy>
    <typemismatch>
        <found>[[ name of the found type, e.g. akka.http.scaladsl.server.StandardRoute ]]</found>
        <required>[[ name of the required type ]]</required>
        <advice>[[ custom error message ]]</advice>
    </typemismatch>
    <typemismatch>
        <found>[[ regular expression, e.g. akka\..*Route ]]</found>
        <required>[[ regular expression for the required type, e.g. .*]]</required>
        <advice>[[ custom error message ]]</advice>
    </typemismatch>
    <notfound>
        <what>[[ what is not found, e.g. value wire ]]</what>
        <advice>[[ custom error message ]]</advice>
    </notfound>
    <notfound>
        <what>[[ regular expression for what is not found, e.g. value wir.* ]]</what>
        <advice>[[ custom error message ]]</advice>
    </notfound>
</clippy>
````

# Contributing to the project

You can submit the compilation errors you encounter & advice proposals here:

Or you can help developing the plugin and/or the UI for submitting new advices! The module structure is:

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

Currently deployed on `https://scala-clippy.herokuapp.com`

In the Heroku app, `JAVA_OPTS` must contain `-Dhttp.port=$PORT` and the `DATABASE_NAME` environmental variable should
be set to `pg` if you want to use postgres.