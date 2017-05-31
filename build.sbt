import sbtrelease.ReleaseStateTransformations._

scalaVersion := "2.10.6" // it's a sbt project

val javacSettings = Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-Xlint:deprecation",
  "-Xlint:unchecked"
)

val internal = "envisia" at "https://maven.envisia.de/internal"
val internalSnapshots = "envisia-snapshots" at "https://maven.envisia.de/internal-snapshots"

lazy val commonSettings = Seq(
  organization := "de.envisia",
  scalaVersion := "2.10.6",
  scalacOptions in(Compile, doc) ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",

    "-Ywarn-nullary-unit",
    "-Xfatal-warnings",
    "-Xlint",
    "-Ywarn-dead-code"
  ),
  javacOptions in(Compile, doc) ++= javacSettings,
  javacOptions in Test ++= javacSettings,
  javacOptions in IntegrationTest ++= javacSettings,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishTo := Some(if (isSnapshot.value) internalSnapshots else internal)
)

val disableDocs = Seq[Setting[_]](
  sources in(Compile, doc) := Seq.empty,
  publishArtifact in(Compile, packageDoc) := false
)

val disablePublishing = Seq[Setting[_]](
  publishArtifact := false,
  // The above is enough for Maven repos but it doesn't prevent publishing of ivy.xml files
  publish := {},
  publishLocal := {},
  com.typesafe.sbt.pgp.PgpKeys.publishSigned := {},
  com.typesafe.sbt.pgp.PgpKeys.publishLocalSigned := {}
)

lazy val shadeAssemblySettings = commonSettings ++ Seq(
  assemblyOption in assembly ~= (_.copy(includeScala = false)),
  test in assembly := {},
  assemblyOption in assembly ~= {
    _.copy(includeScala = false)
  },
  assemblyJarName in assembly := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((epoch, major)) =>
        s"${name.value}.jar" // we are only shading java
      case _ =>
        sys.error("Cannot find valid scala version!")
    }
  },
  target in assembly := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((epoch, major)) =>
        baseDirectory.value.getParentFile / "target" / s"$epoch.$major"
      case _ =>
        sys.error("Cannot find valid scala version!")
    }
  }
)

import scala.xml.transform.{ RewriteRule, RuleTransformer }
import scala.xml.{ Elem, NodeSeq, Node => XNode }

def dependenciesFilter(n: XNode) = new RuleTransformer(new RewriteRule {
  override def transform(n: XNode): NodeSeq = n match {
    case e: Elem if e.label == "dependencies" => NodeSeq.Empty
    case other => other
  }
}).transform(n).head

//---------------------------------------------------------------
// Shaded Clousre-Compiler implementation
//---------------------------------------------------------------

lazy val `shaded-closure-compiler` = project.in(file("shaded/closure-compiler"))
    .settings(commonSettings)
    .settings(shadeAssemblySettings)
    .settings(
      libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20170521",
      name := "shaded-closure-compiler"
    )
    .settings(
      // removes _2.10_0.13 from the artifact_id
      crossPaths := false,
      //logLevel in assembly := Level.Debug,
      assemblyShadeRules in assembly := Seq(
        // FIXME:
        ShadeRule.rename("com.google.**" -> "envisia.shaded.google.@0").inAll,
        ShadeRule.rename("google.protobuf.**" -> "envisia.shaded.google.protobuf.@0").inAll,
        ShadeRule.rename("jsinterop.**" -> "envisia.shaded.jsinterop.@0").inAll,
        ShadeRule.rename("org.kohsuke.args4j.**" -> "envisia.shaded.org.kohsuke.args4j.@0").inAll
      ),

      // https://stackoverflow.com/questions/24807875/how-to-remove-projectdependencies-from-pom
      // Remove dependencies from the POM because we have a FAT jar here.
      makePomConfiguration := makePomConfiguration.value.copy(process = dependenciesFilter),
      //ivyXML := <dependencies></dependencies>,
      //ivyLoggingLevel := UpdateLogging.Full,
      //logLevel := Level.Debug,

      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeBin = false, includeScala = false),
      packageBin in Compile := assembly.value
    )


// Make the shaded version of AHC available downstream
val shadedClosureSettings = Seq(
  unmanagedJars in Compile += (packageBin in(`shaded-closure-compiler`, Compile)).value
)


//---------------------------------------------------------------
// Shaded aggregate project
//---------------------------------------------------------------

lazy val shaded = Project(id = "shaded", base = file("shaded"))
    .settings(disableDocs)
    .settings(disablePublishing)
    // needed for publishSigned, but is disabled
    .settings(publishTo := Some("envisia-snapshots" at "https://maven.envisia.de/internal-snapshots"))
    .aggregate(`shaded-closure-compiler`)
    .disablePlugins(sbtassembly.AssemblyPlugin)

def addShadedDeps(deps: Seq[xml.Node], node: xml.Node): xml.Node = {
  node match {
    case elem: xml.Elem =>
      val child = if (elem.label == "dependencies") {
        elem.child ++ deps
      } else {
        elem.child.map(addShadedDeps(deps, _))
      }
      xml.Elem(elem.prefix, elem.label, elem.attributes, elem.scope, false, child: _*)
    case _ =>
      node
  }
}

lazy val `sbt-closure` = project.in(file("."))
    .settings(commonSettings)
    .settings(
      organization := "de.envisia.sbt", // SBT Plugin
      name := "sbt-closure",
      publishMavenStyle := false,
      sbtPlugin := true,
      publishTo := {
        if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
        else Some(Resolver.url("internal-sbt", url("https://maven.envisia.de/internal-sbt/"))(Resolver.ivyStylePatterns))
      },
      libraryDependencies += "commons-io" % "commons-io" % "2.5"
    )
    .settings(shadedClosureSettings)
    .settings(
      ivyXML :=
          <dependencies>
            <dependency org="de.envisia" name="shaded-closure-compiler" rev={version.value} conf="compile->default(compile)"/>
          </dependencies>
    )
    .settings(
      // This will not work if you do a publishLocal, because that uses ivy...
      pomPostProcess := {
        (node: xml.Node) =>
          addShadedDeps(List(
            <dependency>
              <groupId>de.envisia</groupId>
              <artifactId>shaded-closure-compiler</artifactId>
              <version>{version.value}</version>
            </dependency>
          ), node)
      }
    )
    .aggregate(`shaded`)
    .disablePlugins(sbtassembly.AssemblyPlugin)

addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.3.0")

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.mavenLocal
)

scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx2048M",
  "-XX:MaxPermSize=512M",
  s"-Dproject.version=${version.value}"
)


releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("shaded/publishLocal", _), enableCrossBuild = true),
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  pushChanges
)