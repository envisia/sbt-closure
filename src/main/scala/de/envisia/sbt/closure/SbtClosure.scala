package de.envisia.sbt.closure

import com.typesafe.sbt.web._
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.Keys._
import sbt._

import scala.collection.mutable.ListBuffer


object SbtClosure extends AutoPlugin {
  override def requires = SbtWeb

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import Closure._

  override def projectSettings = Seq(
    flags := ListBuffer.empty[String],
    suffix := ".min.js",
    parentDir := "closure-compiler",
    includeFilter in closure := new UncompiledJsFileFilter(suffix.value),
    excludeFilter in closure := HiddenFileFilter,
    closure := closureCompile.value
  )

  private def invokeCompiler(src: File, target: File, flags: Seq[String]): Unit = {
    val opts = Seq(s"--js=${src.getAbsolutePath}", s"--js_output_file=${target.getAbsolutePath}") ++
      flags.filterNot(s => s.trim.startsWith("--js=") || s.trim.startsWith("--js_output_file="))
    val compiler = new SbtClosureCommandLineRunner(opts.toArray)
    if (compiler.shouldRunCompiler())
      compiler.compile()
    else
      sys.error("Invalid closure compiler configuration, check flags")
  }

  private def closureCompile: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings: Seq[PathMapping] =>
      val targetDir = (public in Assets).value / parentDir.value
      val compileMappings = mappings.view
        .filter(m => (includeFilter in closure).value.accept(m._1))
        .filterNot(m => (excludeFilter in closure).value.accept(m._1))
        .toMap

      // Only do work on files which have been modified
      val runCompiler = FileFunction.cached(streams.value.cacheDirectory / parentDir.value, FilesInfo.hash) { files =>
        files.map { f =>
          val outputFileSubPath = IO.split(compileMappings(f))._1 + suffix.value
          val outputFile = targetDir / outputFileSubPath
          IO.createDirectory(outputFile.getParentFile)
          streams.value.log.info(s"Closure compiler executing on file ${compileMappings(f)}")
          invokeCompiler(f, outputFile, flags.value)
          outputFile
        }
      }

      val compiled = runCompiler(compileMappings.keySet).map { outputFile =>
        val relativePath = IO.relativize(targetDir, outputFile).getOrElse {
          sys.error(s"Cannot find $outputFile path relative to $targetDir")
        }
        (outputFile, relativePath)
      }.toSeq

      compiled ++ mappings.filter {
        // Handle duplicate mappings
        case (mappingFile, mappingName) =>
          val include = compiled.filter(_._2 == mappingName).isEmpty
          if (!include)
            streams.value.log.warn(s"Closure compiler encountered a duplicate mapping for $mappingName and will " +
              "prefer the closure compiled version instead. If you want to avoid this, make sure you aren't " +
              "including minified and non-minified sibling assets in the pipeline.")
          include
      }
  }
}
