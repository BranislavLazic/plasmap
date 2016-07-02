name := "processing"

organization := Common.Organization

version := Common.PlasmapVersion

scalaVersion := Common.ScalaVersion

resolvers ++= Common.resolvers

//fork in run := true

//connectInput in run := true

//javaOptions in run ++= Seq(
//javaOptions ++= Seq(
  //"-Xmx1024m",
  //"-agentpath:/var/plasmap/code/plasmap/libjprofilerti.so=nowait,port=9000",
  //"-XX:+PrintFlagsFinal"
//)

libraryDependencies ++= Seq(
  "com.twitter" %% "util-codec" % "6.23.0",
  "com.softwaremill.reactivekafka" % "reactive-kafka-core_2.11" % "0.8.8",
  "org.apache.commons" % "commons-compress" % "1.8.1",
  "com.twitter" %% "util-codec" % "6.23.0",
  Common.Dependencies.Geow,
  Common.Dependencies.TypesafeConfig,
  Common.Dependencies.ScalaLogging
)

libraryDependencies ++= Common.Dependencies.Akka.All

libraryDependencies ++= Common.TestDependencies.All

//aspectjSettings
//
//javaOptions <++= AspectjKeys.weaverOptions in Aspectj
//
//fork in run := true

libraryDependencies ++= Common.Dependencies.Kamon.All
