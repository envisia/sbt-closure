package de.envisia.sbt.closure

import java.io.File
import java.net.URLClassLoader


class SbtClosureLoader(src: java.util.List[File]) {

  val resource = this.getClass.getResource("/closure-wrapper.jar")
  val resources = Array(resource)

  val parent = this.getClass.getClassLoader
  val closureLoader = new URLClassLoader(resources, null)

  val classLoader = new SbtClosureClassLoader(closureLoader, parent)

  val clazz = classLoader.loadClass("de.envisia.closure.SbtClosureCommandLineRunner")
  val method = clazz.getMethod("compile", classOf[java.util.List[_]])
  val instance = clazz.newInstance()

  def compile: String = {
    method.invoke(instance, src).asInstanceOf[String]
  }

}
