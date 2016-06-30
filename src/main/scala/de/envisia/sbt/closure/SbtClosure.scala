package de.envisia.sbt.closure

import java.time.{ Duration, Instant }

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import sbt.Keys._
import sbt._

import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }


object SbtClosure extends AutoPlugin {
  override def requires: Plugins = SbtWeb

  override def trigger: PluginTrigger = AllRequirements

  object autoImport {

    object ClosureKeys {
      val closure = TaskKey[Seq[File]]("closure", "Generate js files from es6 js files.")
      val closureDebug = SettingKey[Boolean]("closureDebug", "Print files to console")
      val advancedCompilation = SettingKey[Boolean]("advancedComplilation", "wheter or not closure should use advanced compilation.")
      val generateSourceMaps = SettingKey[Boolean]("generateSourceMaps", "Where or not source map files should be generated.")
    }

  }

  import autoImport.ClosureKeys._

  override lazy val buildSettings = Seq(
    generateSourceMaps := true,
    advancedCompilation := false,
    closureDebug := false
  )

  private def invokeCompiler(classesDir: File, src: Seq[File], target: File, flags: Seq[String], sourceMap: Option[String]): Unit = {
    val opts = src.map(_.toString).map(v => s"--js=$v") ++ flags ++ Seq(s"--js_output_file=${target.toString}")

    val parentDir = target.getParentFile
    if (!parentDir.exists()) {
      parentDir.mkdirs()
    }

    val closure = new Closure(classesDir)
    closure.run(opts.toArray, target, sourceMap)
  }

  val baseSbtClosureSettings = Seq(
    includeFilter in closure := "*.js" || "*.ts",
    excludeFilter in closure := HiddenFileFilter || "_*",
    managedResourceDirectories += (resourceManaged in closure in Assets).value,
    resourceManaged in closure in Assets := webTarget.value / "closure" / "main",
    resourceGenerators in Assets <+= closure in Assets,

    closure in Assets := Def.task {
      val sourceDir = (resourceDirectory in Assets).value / "app"
      val targetDir = (resourceManaged in closure in Assets).value
      val classesDir = (classDirectory in Compile).value

      val target = targetDir / "main.min.js"
      val sourceMapName = "main.min.map"
      val sourceMapTarget = targetDir / sourceMapName
      val compilationLevel = if (advancedCompilation.value) "ADVANCED" else "SIMPLE"

      val sources = (sourceDir ** ((includeFilter in closure in Assets).value -- (excludeFilter in closure in Assets).value)).get

      val sm = if (generateSourceMaps.value) {
        Seq(
          s"--entry_point=${sourceDir.toString}/main.js",
          s"--js_module_root=${sourceDir.toString}",
          s"--create_source_map=${sourceMapTarget.getAbsolutePath}",
          s"--source_map_location_mapping=${sourceDir.getParent}|/assets",
          s"--source_map_location_mapping=$targetDir|/assets"
        )
      } else Seq()

      val flags = Seq(
        s"--compilation_level=$compilationLevel",
        "--angular_pass",
        "--language_in=ECMASCRIPT6_TYPED",
        "--language_out=ECMASCRIPT5_STRICT"
      ) ++ sm

      val optionalSourceMap = if (generateSourceMaps.value) Option(sourceMapName) else None

      if (sources.nonEmpty) {
        implicit val fileHasherIncludingOptions: OpInputHasher[File] =
          OpInputHasher[File](f => OpInputHash.hashString(f.getCanonicalPath))

        val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sources) {
          modifiedSources: Seq[File] =>
            val startInstant = Instant.now

            if (modifiedSources.nonEmpty) {
              streams.value.log.info(s"Closure compiling on ${modifiedSources.size} source(s)")
            }

            val compilationResults: Map[File, Try[File]] = {
              if (modifiedSources.nonEmpty) {
                invokeCompiler(classesDir, sources, target, flags, optionalSourceMap)
                modifiedSources.map(inputFile => inputFile -> Success(inputFile)).toMap
              } else {
                Map()
              }
            }

            val opResults: Map[File, OpResult] = compilationResults.mapValues {
              case Success(result) => OpSuccess(sources.toSet, Set(target))
              case Failure(_) => OpFailure
            }

            val duration = Duration.between(startInstant, Instant.now).toMillis

            val createdFiles = if (generateSourceMaps.value) Seq(target, sourceMapTarget) else Seq(target)
            if (modifiedSources.nonEmpty) {
              streams.value.log.info(s"Closure compilation done in $duration ms. ${createdFiles.size} resulting js files(s)")
            }

            (opResults, createdFiles)
        }(fileHasherIncludingOptions)

        (results._1 ++ results._2.toSet).toSeq
      } else {
        Seq()
      }
    }.dependsOn(WebKeys.webModules in Assets).value
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtClosureSettings)

}
