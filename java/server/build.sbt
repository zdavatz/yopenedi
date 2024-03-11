name := """yopenedi"""
organization := "com.ywesee"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.13"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test

libraryDependencies += "commons-io" % "commons-io" % "2.7"
libraryDependencies += "org.bouncycastle" % "bcprov-jdk18on" % "1.77"
libraryDependencies += "org.bouncycastle" % "bcmail-jdk18on" % "1.77"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.ywesee.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.ywesee.binders._"
