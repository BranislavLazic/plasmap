import sbt.Keys._
import sbt._


//IMPORTANT: You need to install phantomjs for the tests to work
//Mac: brew install phantomjs

name := "query-model"

organization := Common.Organization

resolvers ++= Common.resolvers


val vers = "0.6.1-SNAPSHOT"

version := vers

lazy val queryModel = crossProject.in(file("query-model")).
  settings(
    name := "query-model",
    version := vers,
    scalaVersion := Common.ScalaVersion
  ).
  jvmSettings().
  jsSettings(
    // use PhantomJS for testing, because we need real browser JS stuff like TypedArrays
    scalaJSStage in Global := FastOptStage,
    jsDependencies += RuntimeDOM
  )

libraryDependencies += "com.lihaoyi" %% "upickle" % "0.3.8"

libraryDependencies += "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.2"

lazy val queryModelJVM = queryModel.jvm
lazy val queryModelJS  = queryModel.js
