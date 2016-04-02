name := "AKGraph"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0.3",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.2",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.2"
)

