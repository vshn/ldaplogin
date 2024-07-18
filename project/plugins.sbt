// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.4")

// Defines scaffolding (found under .g8 folder)
// http://www.foundweekends.org/giter8/scaffolding.html
// sbt "g8Scaffold form"
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.16.2")
addDependencyTreePlugin
