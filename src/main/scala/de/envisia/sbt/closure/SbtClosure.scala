package de.envisia.sbt.closure

import java.io.{BufferedWriter, FileWriter}
import java.time.{Duration, Instant}
import java.util.Optional

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import sbt.Keys._
import sbt._

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
  private def invokeCompiler(src: Seq[File], target: File, flags: Seq[String], sourceMap: Optional[String]): Unit = {
    val opts = src.map(_.toString).map(v => s"--js=$v") ++ flags ++ Seq(s"--js_output_file=${target.toString}")

    val parentDir = target.getParentFile
    if (!parentDir.exists()) {
      parentDir.mkdirs()
    }

    val commandLineRunner = new ClosureCommandLineRunner(opts.toArray)
    commandLineRunner.compile(target, sourceMap)


    //    if (!target.exists()) {
    //      target.createNewFile()
    //    }
    //    if (!sourceMapTarget.exists() && withSourceMaps) {
    //      sourceMapTarget.createNewFile()
    //    }
    //
    //    val compiler = new ClosureRunner(List(rootDir.toString), src, sourceMapTarget.toString, rootDir.getParentFile, withSourceMaps)
    //
    //    val res = compiler.compile()
    //    val out = new PrintStream(new FileOutputStream(target))
    //    out.println(res)
    //    if (withSourceMaps) {
    //      out.println(s"//# sourceMappingURL=main.min.js.map")
    //    }
    //    out.close()
    //
    //    if (withSourceMaps) {
    //      val sourceMapOut = new PrintStream(new FileOutputStream(sourceMapTarget))
    //      compiler.sourceMap(sourceMapOut, "main.min.js.map")
    //      sourceMapOut.close()
    //    }
  }

  val baseSbtClosureSettings = Seq(
    includeFilter in closure := "*.js",
    excludeFilter in closure := HiddenFileFilter || "_*",
    managedResourceDirectories += (resourceManaged in closure in Assets).value,
    resourceManaged in closure in Assets := webTarget.value / "closure" / "main",
    resourceGenerators in Assets <+= closure in Assets,

    closure in Assets := Def.task {
      val sourceDir = (resourceDirectory in Assets).value / "app"
      val targetDir = (resourceManaged in closure in Assets).value

      val target = targetDir / "main.min.js"
      val sourceMapName = "main.min.map"
      val sourceMapTarget = targetDir / sourceMapName
      val compilationLevel = if (advancedCompilation.value) "ADVANCED" else "SIMPLE"

      val sources = (sourceDir ** ((includeFilter in closure in Assets).value -- (excludeFilter in closure in Assets).value)).get

      val sm = if (generateSourceMaps.value) {
        Seq(
          s"--create_source_map=${sourceMapTarget.getAbsolutePath}",
          s"--source_map_location_mapping=${sourceDir.getParent}|/assets",
          s"--source_map_location_mapping=$targetDir|/assets"
        )
      } else Seq()

      val flags = Seq(
        s"--compilation_level=$compilationLevel",
        "--angular_pass",
        "--language_in=ECMASCRIPT6",
        "--language_out=ECMASCRIPT5_STRICT"
      ) ++ sm

      val optionalSourceMap = if (generateSourceMaps.value) Optional.of(sourceMapName) else Optional.empty[String]()

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
                try {
                  invokeCompiler(sources, target, flags, optionalSourceMap)
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
