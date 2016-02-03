package de.envisia.sbt.closure

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util

import scala.collection.JavaConversions._


class SbtClosureCommandLineRunner {
  val child = this.getClass.getClassLoader

  val sourceFile: Class[_] = child.loadClass("com.google.javascript.jscomp.SourceFile")
  val compiler: Class[_] = child.loadClass("com.google.javascript.jscomp.Compiler")
  val compilerOptions: Class[_] = child.loadClass("com.google.javascript.jscomp.CompilerOptions")
  val languageMode: Class[_] = child.loadClass("com.google.javascript.jscomp.CompilerOptions$LanguageMode")
  val compilationLevel: Class[_] = child.loadClass("com.google.javascript.jscomp.CompilationLevel")
  val result: Class[_] = child.loadClass("com.google.javascript.jscomp.Result")

  val compilerOptionsInstance = compilerOptions.newInstance()

  val languageModeEnum = languageMode.getEnumConstants
  val ECMASCRIPT5 = languageModeEnum(1)
  val ECMASCRIPT6 = languageModeEnum(3)

  val setLanguageIn = compilerOptions.getDeclaredMethod("setLanguageIn", languageMode)
  val setLanguageOut = compilerOptions.getDeclaredMethod("setLanguageOut", languageMode)

  setLanguageIn.invoke(compilerOptionsInstance, ECMASCRIPT6)
  setLanguageOut.invoke(compilerOptionsInstance, ECMASCRIPT5)

  val compilationLevels = compilationLevel.getEnumConstants
  val second = compilationLevels.apply(1)
  val setOptionsForCompilationLevel = second.getClass.getDeclaredMethod("setOptionsForCompilationLevel", compilerOptions)
  setOptionsForCompilationLevel.invoke(second, compilerOptionsInstance.asInstanceOf[AnyRef])


  val sourceFileConstructor = sourceFile.getConstructor(classOf[String])
  val sourceFileFromCode = sourceFile.getDeclaredMethod("fromCode", classOf[String], classOf[String])
  sourceFileFromCode.setAccessible(true)

  val emptySources: java.util.List[AnyRef] = util.Collections.emptyList()


  val setAngularPass = compilerOptions.getDeclaredMethod("setAngularPass", classOf[Boolean])
  setAngularPass.invoke(compilerOptionsInstance, true: java.lang.Boolean)

  val compilerInstance = compiler.newInstance()
  val compileMethod = compiler.getDeclaredMethod("compile", classOf[java.util.List[_]], classOf[java.util.List[_]], compilerOptions)
  val initMethod = compiler.getDeclaredMethod("init", classOf[java.util.List[_]], classOf[java.util.List[_]], compilerOptions)
  val checkMethod = compiler.getDeclaredMethod("check")

  def compile(src: Seq[(File, String)]): String = {
    val sources: java.util.List[Any] = src.map { case (file, content) =>
      sourceFileFromCode.invoke(null, file.getAbsolutePath, content)
    }

    initMethod.invoke(compilerInstance, emptySources, sources, compilerOptionsInstance.asInstanceOf[AnyRef])

    try {
      // val isValid = checkMethod.invoke(compilerInstance)
      // println(s"Valid: $isValid")

      val result = compileMethod.invoke(compilerInstance, emptySources, sources, compilerOptionsInstance.asInstanceOf[AnyRef])
      val toSource = compiler.getDeclaredMethod("toSource")
      val data = toSource.invoke(compilerInstance).asInstanceOf[String]

      data
    } catch {
      case e: InvocationTargetException =>

        println(s"Cause: ${e.getCause}")
        e.getCause.printStackTrace()
        println("--")
        ""
    }
  }

  def shouldRunCompiler(): Boolean = {
    true
  }

}
