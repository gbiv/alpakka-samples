import sbt.Keys._
crossPaths := false // drop off Scala suffix from artifact names.
autoScalaLibrary := false // exclude scala-library from dependencies
fork / run := true
connectInput / run := true
lazy val distProject = project
  .in(file("."))
  .settings(
    name := "alpakka-sample-kafka-to-websocket-clients",
    organization := "com.lightbend.akka",
    version := "1.3.0",
    scalaVersion := Dependencies.scalaVer,
    libraryDependencies ++= Dependencies.dependencies,
  )
