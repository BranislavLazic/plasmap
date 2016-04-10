name := "util"

organization := Common.Organization

scalaVersion := Common.ScalaVersion

version := Common.PlasmapVersion

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  Common.Dependencies.Play.PlayWs,
  Common.Dependencies.PlayGeoJson,
  Common.Dependencies.TypesafeConfig,
  Common.Dependencies.Geow,
  "org.scalanlp" %% "breeze" % "0.11.2",
  "com.yahoo.datasketches" % "sketches-core" % "0.2.2",
  "org.apache.commons" % "commons-compress" % "1.8.1",
  //"com.softwaremill" %% "reactive-kafka" % "0.5.0",
  "com.softwaremill.reactivekafka" % "reactive-kafka-core_2.11" % "0.8.2",
  "com.twitter" %% "util-codec" % "6.23.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.12" force(),
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.12" force(),
  "com.typesafe.akka" %% "akka-testkit" % "2.3.12" force(),
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0" force(),
  "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0" force(),
  Common.Dependencies.ScalaLogging
)

libraryDependencies ++= Common.Dependencies.GeoTools.All

libraryDependencies ++= Common.TestDependencies.All

