package de.envisia.sbt.closure

import java.io.{FileOutputStream, PrintStream}
import java.time.{Duration, Instant}

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import de.envisia.closure.SbtClosureCommandLineRunner
import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._
import scala.io.Source
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

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

  // Should return something like:
  // java -jar ~/Downloads/compiler-latest/compiler.jar --common_js_entry_module index.module.js
  // --angular_pass --js src/app/index.module.js --js src/app/components/auth-service/auth-service.js
  // --js src/app/components/storage-service/storage-service.js
  private def invokeCompiler(src: Seq[String], target: File, flags: Seq[String], log: Logger): Unit = {
    val opts = src ++ Seq(s"--js_output_file=${target.getAbsolutePath}") ++
      flags.filterNot(s => s.trim.startsWith("--js=") || s.trim.startsWith("--js_output_file="))
    try {


      val compiler = new SbtClosureCommandLineRunner(opts.toArray)
      val runner = true
      log.info(s"Run Compiler: $runner")
      if (runner) {
        compiler.compile()
      } else {
        sys.error("Invalid closure compiler configuration, check flags")
      }
    } catch {
      case e: Exception => log.error(s"Exception: $e"); e.printStackTrace()
    }
  }

  val baseSbtClosureSettings = Seq(
    includeFilter in closure := "*.js",
    excludeFilter in closure := HiddenFileFilter || "_*",
    managedResourceDirectories += (resourceManaged in closure in Assets).value,
    resourceManaged in closure in Assets := webTarget.value / "closure" / "main",
    resourceGenerators in Assets <+= closure in Assets,
    closure in Assets := Def.task {
      val sourceDir = (sourceDirectory in Assets).value
      val targetDir = (resourceManaged in closure in Assets).value

      val target = targetDir / "app.min.js"
      val sourceMapTarget = targetDir / "app.min.js.map"

      // SIMPLE_OPTIMIZATIONS, ADVANCED_OPTIMIZATIONS
      // val compilationLevel = if (advancedCompilation.value) "ADVANCED" else "SIMPLE"
      val compilationLevel = if (advancedCompilation.value) "ADVANCED_OPTIMIZATIONS" else "SIMPLE_OPTIMIZATIONS"

      val sources = (sourceDir ** ((includeFilter in closure in Assets).value -- (excludeFilter in closure in Assets).value)).get

      // val query = """--module-bind "js=babel?presets=es2015" """.trim
      // val nodeModulePaths = (nodeModuleDirectories in Plugin).value.map(_.getPath)
      // val webpackJsShell = (webJarsNodeModulesDirectory in Plugin).value / "webpack" / "bin" / "webpack.js"
      // val executeWebpack = SbtJsTask

      val flags = Seq(
        s"--compilation_level=$compilationLevel",
        "--common_js_entry_module=index.module",
        "--angular_pass",
        // "--formatting=PRETTY_PRINT",
        s"--create_source_map=${sourceMapTarget.getAbsolutePath}",
        "--language_in=ECMASCRIPT6",
        "--language_out=ECMASCRIPT5"
      )

      if (sources.nonEmpty) {
        implicit val fileHasherIncludingOptions: OpInputHasher[File] =
          OpInputHasher[File](f => OpInputHash.hashString(f.getCanonicalPath))

        val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sources) {
          modifiedSources: Seq[File] =>
            val startInstant = Instant.now

            if (modifiedSources.nonEmpty) {
              streams.value.log.info(s"Closure compiling on ${modifiedSources.size} source(s")
            }


            val compilationResults: Map[File, Try[File]] = {
              if (modifiedSources.nonEmpty) {
                try {
                  invokeCompiler(sources.map(file => s"--js=${file.getAbsolutePath}"), target, flags, streams.value.log)
                  modifiedSources.map(inputFile => inputFile -> Success(inputFile)).toMap
                } catch {
                  case e: Exception => modifiedSources.map(inputFile => inputFile -> Failure(e)).toMap
                }
              } else {
                Map()
              }
            }

            val opResults: Map[File, OpResult] = compilationResults.mapValues {
              case Success(result) => OpSuccess(sources.toSet, Set(target))
              case Failure(_) => OpFailure
            }

            val duration = Duration.between(startInstant, Instant.now).toMillis

            val createdFiles = Seq(target, sourceMapTarget)
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
