name := "query-engine-macros"

organization := Common.Organization

scalaVersion := "2.11.7"

version := Common.PlasmapVersion

resolvers ++= Common.resolvers

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.7"
) ++ Common.TestDependencies.All

