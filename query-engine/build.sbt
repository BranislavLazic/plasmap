name := """query-engine"""

organization := Common.Organization

version := Common.PlasmapVersion

scalaVersion := Common.ScalaVersion

resolvers ++= Common.resolvers

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.2.0-RC4",
  "com.github.mpilquist" %% "simulacrum" % "0.4.0"
)

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "upickle" % "0.3.6",
  "com.janschulte" %% "akvokolekta" % "0.1.0-SNAPSHOT",
  Common.Dependencies.ScalaLogging,
  Common.Dependencies.Geow,
  Common.Dependencies.ScalazStream,
  "org.scalamock" %% "scalamock-core" % "3.2",
  "com.twitter" %% "util-codec" % "6.23.0",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "flot" % "0.8.0"
)

libraryDependencies ++= Common.Dependencies.Akka.All

libraryDependencies ++= Common.TestDependencies.All

parallelExecution in Test := false