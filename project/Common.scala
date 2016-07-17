import sbt.IvyConsole.Dependencies
import sbt._
import Keys._

object Common {

  val settings = net.virtualvoid.sbt.graph.Plugin.graphSettings

  val resolvers = Seq(
    Resolvers.Sonatype.Releases,
    Resolvers.Sonatype.Snapshots,
    Resolvers.Akka.Snapshots,
    Resolvers.Typesafe.Releases,
    Resolvers.OSGeo.Releases,
    Resolvers.Bintray.Maven,
    Resolvers.Bintray.Scalaz,
    Resolvers.Bintray.Redis
  )

  def Organization = "io.plasmap"

  def ScalaVersion = "2.11.8"

  def PlasmapVersion = "0.5-SNAPSHOT"

  object Dependencies {

    object Akka {
      private val orga = "com.typesafe.akka"
      private val version = "2.4.7"

      val Actor = orga %% "akka-actor" % version
      val Http = orga %% "akka-http-core" % version
      val Stream = orga %% "akka-stream" % version

      val TestKit = orga %% "akka-testkit" % version
      val StreamTestkit = orga %% "akka-stream-testkit" % version
      val HttpTestkit = orga %% "akka-http-testkit" % version

      val Logback = "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime"
      val Slf4j = orga %% "akka-slf4j" % version
      val Config = "com.typesafe" % "config" % "1.2.1"

      val All = Seq(Actor, Http, Stream, TestKit, StreamTestkit, HttpTestkit, Logback, Slf4j, Config)
    }

    object Play {
      private val orga = "com.typesafe.play"
      private val version = "2.4.0-M2"
      val PlayWs = orga %% "play-ws" % version
    }

    val PlayGeoJson = "com.typesafe.play.extras" %% "play-geojson" % "1.2.0"

    val ScalazStream = "org.scalaz.stream" %% "scalaz-stream" % "0.7.1a"

    object Kamon {
      private val orga = "io.kamon"
      private val version = "0.3.5"
      val Core = orga %% "kamon-core" % version
      val Statsd = orga %% "kamon-statsd" % version
      val LogReporter = orga %% "kamon-log-reporter" % version
      val SystemMetrics = orga %% "kamon-system-metrics" % version

      val All = Seq(Core, Statsd, LogReporter, SystemMetrics, AspectJ)
    }

    val AspectJ = "org.aspectj" % "aspectjweaver" % "1.8.5"

    val TypesafeConfig = "com.typesafe" % "config" % "1.2.1"
    val ScalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

    val Geow = "io.plasmap" %% "geow" % "0.3.17-SNAPSHOT"

    object GeoTools {

      private val orga = "org.geotools"
      private val version = "12-RC1"
      val Shapefile = orga % "gt-shapefile" % version
      val Swing = orga % "gt-swing" % version

      val All = Seq(Shapefile, Swing)
    }

    object DB {

      val Redis = "com.etaty.rediscala" %% "rediscala" % "1.4.0"

      val All = Seq(Redis)
    }

  }

  // "test" missing on purpose
  object TestDependencies {

    object Specs2 {

      private val orga = "org.specs2"
      private val version = "3.6"

      val Core = orga %% "specs2-core" % version
      val Scalacheck = orga %% "specs2-scalacheck" % version
      val MatcherExtra = orga %% "specs2-matcher-extra" % version
      val JUnit = orga %% "specs2-junit" % version

      val All: Seq[ModuleID] = Seq(Core, Scalacheck, MatcherExtra, JUnit)
    }

    val Scalameter = "com.storm-enroute" %% "scalameter" % "0.6"

    val Scalacheck = "org.scalacheck" %% "scalacheck" % "1.12.2"

    object ScalaMock {
      val All = "org.scalamock" %% "scalamock-specs2-support" % "3.2.1"
    }

    val ScalaTest = "org.scalatest" % "scalatest_2.11" % "2.2.4"

    val All: Seq[ModuleID] = Specs2.All ++ Seq(ScalaMock.All, ScalaTest, Scalameter)
  }

  object Resolvers {

    object Typesafe {
      val Releases = "Typesafe Release repository" at "http://repo.typesafe.com/typesafe/releases/"
      val Snapshots = "Typesafe Snapshot repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    }

    object Akka {
      val Snapshots = "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
    }

    object Bintray {
      val Scalaz = "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
      val Maven = "bintray/non" at "http://dl.bintray.com/non/maven"
      val Redis = "rediscala" at "http://dl.bintray.com/etaty/maven"
    }

    object Sonatype {
      val Releases = Resolver.sonatypeRepo("releases")
      val Snapshots = Resolver.sonatypeRepo("snapshots")
    }

    object OSGeo {
      val Releases = "OSGeo Release repository" at "http://download.osgeo.org/webdav/geotools"
    }

  }

}