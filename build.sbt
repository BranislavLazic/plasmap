import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._

import sbt.Project.projectToRef

name := "plasmap"

description := "Streaming OpenStreetMap API"

organization in ThisBuild := "io.plasmap"

scalaVersion in ThisBuild := Common.ScalaVersion

promptTheme := ScalapenosTheme

//If we wanted to publish cross compiled versions. Might be interesting for geow.
//crossScalaVersions in ThisBuild := Seq("2.10.4", "2.11.0")

//scalacOptions in ThisBuild ++= Seq(
//  "-feature",
//  "-deprecation",
//  "-Yno-adapted-args",
//  "-Ywarn-value-discard",
//  // "-Ywarn-dead-discardcode", // busted in 2.11 it seems
//  "-Xlint",
//  "-Xfatal-warnings",
//    "-unchecked"
//  )

// Let's be even more picky in non-test code
//scalacOptions in compile ++= Seq(
//  "-Yno-imports",
//  "-Ywarn-numeric-widen"
//)

//javaOptions in run += "-XX:+HeapDumpOnOutOfMemoryError
/*
javaOptions in run ++= Seq(
  //"-agentpath:yjp-2015-build-15076/bin/linux-x86-64/libyjpagent.so=port=10001",
  "/var/plasmap/code/plasmap/jprofiler9/bin/linux-x64/libjprofilerti.so=nowait,port=10001",
  "-XX:+PrintFlagsFinal"
)*/

//resolvers ++= Common.resolvers

// Commons
lazy val commonsUtil = project.in(file("util"))

lazy val dal = project.in(file("dal")).dependsOn(commonsUtil)

//Processing
lazy val processing = project.in(file("processing")).dependsOn(dal, commonsUtil)//.enablePlugins(YourKit,JavaAppPackaging)

lazy val queryModel = project.in(file("query-model")).enablePlugins(ScalaJSPlugin)

lazy val queryEngineMacros = project.in(file("query-engine-macros")).settings(Common.settings:_*)

lazy val queryEngine = project.in(file("query-engine")).settings(Common.settings:_*).dependsOn(queryEngineMacros, dal, commonsUtil, queryModel).enablePlugins(SbtPrompt,ShPlugin)

lazy val plasmapJS = project.in(file("plasmapjs")).enablePlugins(ScalaJSPlugin)


publishArtifact := false

updateOptions := updateOptions.value.withCachedResolution(true)

logBuffered := false

parallelExecution in Test := false

