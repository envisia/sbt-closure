package de.envisia.sbt.closure

import java.io.{ BufferedWriter, FileWriter, PrintWriter }
import java.net.URLClassLoader

object Closure {

  val loader: URLClassLoader = new ClosureClassLoader(Array(this.getClass.getResource("/closure/compiler-v20160619.jar")))

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
