name := "corrector"

version := "1.0"

scalaVersion := "2.12.1"

mainClass in assembly := Some("WebServer")

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "net.lingala.zip4j" % "zip4j" % "1.3.2",
  "com.typesafe.akka" %% "akka-http" % "10.0.1",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.1"
)
    