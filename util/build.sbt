name := "util"

organization := Common.Organization

scalaVersion := Common.ScalaVersion

version := Common.PlasmapVersion

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  Common.Dependencies.TypesafeConfig,
  Common.Dependencies.Geow,
  "org.scalanlp" %% "breeze" % "0.11.2",
  "com.yahoo.datasketches" % "sketches-core" % "0.2.2",
  "org.apache.commons" % "commons-compress" % "1.8.1",
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.8.8",
  "com.twitter" %% "util-codec" % "6.23.0",
  Common.Dependencies.ScalaLogging
)

libraryDependencies ++= Common.Dependencies.Akka.All

libraryDependencies ++= Common.Dependencies.GeoTools.All

libraryDependencies ++= Common.TestDependencies.All

