addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

libraryDependencies <++= (sbtVersion) {
  sv => Seq(
    "org.scala-sbt" % "scripted-plugin" % sv
  )
}

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")