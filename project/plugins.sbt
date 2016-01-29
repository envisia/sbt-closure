addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

libraryDependencies <++= (sbtVersion) {
  sv => Seq(
    "org.scala-sbt" % "scripted-plugin" % sv
  )
}
