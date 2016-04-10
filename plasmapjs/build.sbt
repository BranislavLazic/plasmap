enablePlugins(ScalaJSPlugin)

organization := Common.Organization

name := "plasmapjs"

version := "0.6.1-SNAPSHOT"

scalaVersion := Common.ScalaVersion

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

libraryDependencies ++= Seq(
    "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.3",
    "io.plasmap"           %%% "query-model" % "0.6.1-SNAPSHOT",
    "org.scala-js"         %%% "scalajs-dom" % "0.8.2",
    "com.lihaoyi"          %%% "upickle"     % "0.3.8",
    "com.lihaoyi"          %%% "autowire"    % "0.2.5",
    "com.lihaoyi"          %%% "utest"       % "0.3.0" % "test",
    "io.plasmap"           %%% "pamphlet"    % "0.8-SNAPSHOT"
)

scalaJSStage in Global := FastOptStage

//Needed when using jQuery
jsDependencies += RuntimeDOM

testFrameworks += new TestFramework("utest.runner.Framework")
