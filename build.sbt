name := """ldaplogin"""
organization := "vshn.net"

version := "1.0-SNAPSHOT"

Compile/doc/sources := Seq.empty
Compile/packageDoc/publishArtifact := false

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.16"

libraryDependencies += guice
libraryDependencies += "org.apache.directory.server" % "apacheds-protocol-ldap" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.server" % "apacheds-core" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.server" % "apacheds-core-api" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.server" % "apacheds-core-shared" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.server" % "apacheds-protocol-shared" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.server" % "apacheds-server-config" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.server" % "apacheds-service-builder" % "2.0.0.AM27"
libraryDependencies += "org.apache.directory.api" % "api-ldap-net-mina" % "2.1.7"
libraryDependencies += "org.apache.directory.api" % "api-ldap-codec-standalone" % "2.1.7"

// OAuth
libraryDependencies += "com.google.oauth-client" % "google-oauth-client-java6" % "1.39.0"
libraryDependencies += "com.google.http-client" % "google-http-client-gson" % "1.46.3"
libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.39.0"

// MongoDB/Morphia
libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.11.5"
libraryDependencies += "dev.morphia.morphia" % "morphia-core" % "2.4.14"

gzip / includeFilter := "*.css" || "*.js"
pipelineStages := Seq(digest, gzip)
