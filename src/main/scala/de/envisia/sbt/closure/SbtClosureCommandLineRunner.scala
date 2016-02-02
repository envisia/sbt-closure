package de.envisia.sbt.closure

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile


private class SbtClosureCommandLineRunner(args: Array[String]) {
  val resource = this.getClass.getResource("/closure-wrapper.jar")
  println(resource)
  val resources = Array(resource)

  val child: URLClassLoader = new URLClassLoader(resources, this.getClass.getClassLoader)

  val classToLoad: Class[_] = child.loadClass("de.envisia.closure.ClosureWrapper")

  val constructor = classToLoad.getConstructor(args.getClass)

  val instance = constructor.newInstance(args)

  val method1 = classToLoad.getDeclaredMethod("compile")
  val method2 = classToLoad.getDeclaredMethod("shouldRunCompiler")

  def compile(): Unit = {
    method1.invoke(instance)
  }

  def shouldRunCompiler(): Boolean = {
    method2.invoke(instance).asInstanceOf[Boolean]
  }

}
