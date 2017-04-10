libraryDependencies <++= (sbtVersion) {
  sv => Seq(
    "org.scala-sbt" % "scripted-plugin" % sv
  )
}

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

addSbtPlugin("com.gilt" % "sbt-dependency-graph-sugar" % "0.8.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")