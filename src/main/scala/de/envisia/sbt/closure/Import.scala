package de.envisia.sbt.closure

import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.{SettingKey, TaskKey}


object Import {
  val closure = TaskKey[Pipeline.Stage]("closure", "Runs JavaScript web assets through the Google closure compiler")

  object Closure {
    val flags = SettingKey[Seq[String]]("closure-flags", "Command line flags to pass to the closure compiler, example: Seq(\"--formatting=PRETTY_PRINT\", \"--accept_const_keyword\")")
    val suffix = SettingKey[String]("closure-suffix", "Suffix to append to compiled files, default: \".min.js\"")
    val parentDir = SettingKey[String]("closure-parent-dir", "Parent directory name where closure compiled JS will go, default: \"closure-compiler\"")
  }
}
