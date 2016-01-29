package de.envisia.sbt.closure

import com.google.javascript.jscomp.CommandLineRunner


private class SbtClosureCommandLineRunner(args: Array[String]) extends CommandLineRunner(args) {
  def compile(): Unit = doRun()
}
