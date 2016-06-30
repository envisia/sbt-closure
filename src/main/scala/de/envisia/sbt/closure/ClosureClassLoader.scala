package de.envisia.sbt.closure

import java.net.{ URL, URLClassLoader }
import java.util
import java.util.jar.JarFile

class ClosureClassLoader(urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, parent) {

  def this(urls: Array[URL]) = this(urls, ClassLoader.getSystemClassLoader)

  @throws[ClassNotFoundException]
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    // This will actually load all Closure internals with the Closure Loader
    if (name.startsWith("java")) {
      parent.loadClass(name)
    } else {
      super.loadClass(name, resolve)
    }
  }

}
