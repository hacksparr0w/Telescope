name := "telescope"
version := "0.1.0"
description := "Sponge plugin providing server observability with Prometheus"
organization := "me.hacksparr0w"
scalaVersion := "3.3.3"

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"

crossPaths := false

resolvers += "sponge" at "https://repo.spongepowered.org/repository/maven-public/"

libraryDependencies += "org.spongepowered" % "spongeapi" % "11.0.0" % "provided"

libraryDependencies += "io.prometheus" % "prometheus-metrics-core" % "1.0.0"
libraryDependencies += "io.prometheus" % "prometheus-metrics-instrumentation-jvm" % "1.0.0"
libraryDependencies += "io.prometheus" % "prometheus-metrics-exporter-httpserver" % "1.0.0"
