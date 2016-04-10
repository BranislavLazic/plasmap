name := """query-engine"""

organization := Common.Organization

version := Common.PlasmapVersion

scalaVersion := Common.ScalaVersion

resolvers ++= Common.resolvers

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

libraryDependencies ++= Seq(
  //"com.assembla.scala-incubator" %% "graph-core" % "1.9.3-SNAPSHOT",
  //"com.assembla.scala-incubator" %% "graph-constrained" % "1.9.3-SNAPSHOT",
  "com.chuusai" %% "shapeless" % "2.2.0-RC4",
  "com.github.mpilquist" %% "simulacrum" % "0.4.0"
)

libraryDependencies ++= Seq(
  ws, // Play's web services module
  "com.typesafe.akka" %% "akka-actor" % "2.3.12" force(),
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.12" force(),
  "com.typesafe.akka" %% "akka-testkit" % "2.3.12" force(),
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0" force(),
  "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0" force(),
  "com.lihaoyi" %% "upickle" % "0.3.6",
  Common.Dependencies.ScalaLogging,
  Common.Dependencies.Geow,
  Common.Dependencies.ScalazStream,
  "org.scalamock" %% "scalamock-core" % "3.2",
  "com.twitter" %% "util-codec" % "6.23.0",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "flot" % "0.8.0"
)

libraryDependencies ++= Common.TestDependencies.All

parallelExecution in Test := false