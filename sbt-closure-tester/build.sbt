import de.envisia.sbt.closure._
lazy val root = (project in file(".")).enablePlugins(SbtWeb)

ClosureKeys.generateSourceMaps := true