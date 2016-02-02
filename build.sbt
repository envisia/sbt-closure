sbtPlugin := true

organization := "de.envisia.sbt"

name := "sbt-closure"

version := "0.0.11-M61"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "org.webjars.npm" % "webpack" % "1.12.10",
  "org.webjars.npm" % "babel-core" % "6.4.5",
  "org.webjars.npm" % "babel-loader" % "6.2.1",
  "org.webjars.npm" % "babel-preset-es2015" % "6.3.13"
)

libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20151216"
// ScalaJS 0.6.6
// libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20130603"

addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.1.1")

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.mavenLocal
)

publishMavenStyle := false

scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx2048M",
  "-XX:MaxPermSize=512M",
  s"-Dproject.version=${version.value}"
)


bintrayOrganization := Some("envisia")

pomExtra := (
  <url>https://github.com/envisia/sbt-closure</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:envisia/sbt-closure.git</url>
      <connection>scm:git:git@github.com:envisia/sbt-closure.git</connection>
    </scm>
    <developers>
      <developer>
        <id>envisia</id>
        <name>Christian Schmitt</name>
        <url>https://www.envisia.de</url>
      </developer>
    </developers>)
