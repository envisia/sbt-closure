package de.envisia.sbt.closure

import java.io.{ BufferedWriter, File, FileWriter, PrintWriter }
import java.net.URLClassLoader

import org.apache.commons.io.FileUtils

class Closure(classesDirectory: File) {

  // To have a classloader load a JAR file it can't be inside another jar so we first need to
  // copy the file to a sane directory and then load the jar file from there.
  // This also means we cache that jar file there since else every compile would copy it.
  val cachedFile = classesDirectory.toPath.resolve("cached-closure-compiler-v20160619.jar").toFile
  val url = this.getClass.getResource("/closure-compiler-v20160619.jar")
  if (!cachedFile.exists()) {
    FileUtils.copyURLToFile(url, cachedFile)
  }

  val loader: URLClassLoader = new ClosureClassLoader(Array(cachedFile.toURI.toURL))

  def run(args: Array[String], target: sbt.File, sourceMap: Option[String]) = {
    val clazz: Class[_] = loader.loadClass("com.google.javascript.jscomp.CommandLineRunner")

    // Gets the constructor and instanciates it
    val constructor = clazz.getDeclaredConstructor(classOf[Array[String]])
    constructor.setAccessible(true)
    val instance = constructor.newInstance(args)

    // The superclass contains the doRun method
    val superClazz = clazz.getSuperclass
    val method = superClazz.getDeclaredMethod("doRun")
    method.setAccessible(true)

    // Invokes the doRun method
    method.invoke(instance)

    sourceMap.filter(_ => target.exists()).foreach { data =>
      val out = new PrintWriter(new BufferedWriter(new FileWriter(target, true)))
      try {
        out.print("//# sourceMappingURL=")
        out.print(data)
      } finally out.close()
    }
  }

}
