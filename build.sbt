import play.Project._

name := """play-actor-realtime"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.1", 
  "org.webjars" % "bootstrap" % "2.3.1"
)

playScalaSettings
