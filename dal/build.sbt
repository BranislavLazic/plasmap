name := "dal"

organization := Common.Organization

version := Common.PlasmapVersion

scalaVersion := Common.ScalaVersion

resolvers ++= Common.resolvers

libraryDependencies ++= Seq(
  Common.Dependencies.Geow,
  Common.Dependencies.Akka.Actor,
  Common.Dependencies.TypesafeConfig,
  Common.Dependencies.ScalaLogging,
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.4" exclude("org.xerial.snappy", "snappy-java"),
  "org.xerial.snappy" % "snappy-java" % "1.1.1.6", //https://github.com/ptaoussanis/carmine/issues/5,
  Common.Dependencies.TypesafeConfig,
  Common.Dependencies.Geow,
  Common.Dependencies.DB.Redis,
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "1.7.4"  exclude("org.slf4j","log4j-over-slf4j"),
  "org.reactivemongo" %% "reactivemongo" % "0.11.7",
  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "1.7.4",
  "com.typesafe.slick" %% "slick" % "3.1.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.0",
  "com.zaxxer" % "HikariCP" % "2.4.3",
  "com.github.mauricio" %% "mysql-async" % "0.2.15",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.basho.riak" % "riak-client" % "2.0.0",
  "com.github.nscala-time" %% "nscala-time" % "1.8.0",
  "org.scalaz.stream" %% "scalaz-stream" % "0.6a",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
  "org.scodec" %% "scodec-bits" % "1.0.10",
  "org.scodec" %% "scodec-core" % "1.8.2"
)

libraryDependencies ++= Common.TestDependencies.All

logBuffered := false

parallelExecution in Test := false
