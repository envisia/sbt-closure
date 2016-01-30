package de.envisia.sbt.closure

import java.time.{Duration, Instant}

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import sbt.Keys._
import sbt._

import scala.language.implicitConversions


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
  private def invokeCompiler(src: Seq[String], target: File, flags: Seq[String]): Unit = {
    val opts = src ++ Seq(s"--js_output_file=${target.getAbsolutePath}") ++
      flags.filterNot(s => s.trim.startsWith("--js=") || s.trim.startsWith("--js_output_file="))
    val compiler = new SbtClosureCommandLineRunner(opts.toArray)
    if (compiler.shouldRunCompiler())
      compiler.compile()
    else
      sys.error("Invalid closure compiler configuration, check flags")
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

      val compilationLevel = if (advancedCompilation.value) "ADVANCED" else "SIMPLE"

      val sources = (sourceDir ** ((includeFilter in closure in Assets).value -- (excludeFilter in closure in Assets).value)).get

      val files = sources.map { file => s"--js=${file.getAbsolutePath}" }
      val flags = Seq(
        s"--compilation_level=$compilationLevel",
        "--common_js_entry_module=index.module",
        "--angular_pass",
        "--formatting=PRETTY_PRINT",
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


            val opResults = {
              if (modifiedSources.nonEmpty) {
                try {
                  invokeCompiler(files, target, flags)
                  Map(target -> OpSuccess(sources.toSet, Set(target)))
                } catch {
                  case e: Exception => Map(target -> OpFailure)
                }
              } else {
                Map(target -> OpFailure)
              }
            }

            val duration = Duration.between(startInstant, Instant.now).toMillis

            val createdFiles = Seq(target, sourceMapTarget)
            if (createdFiles.nonEmpty) {
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
