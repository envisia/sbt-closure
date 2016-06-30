package de.envisia.sbt.closure

import java.net.{ URL, URLClassLoader }

class ClosureClassLoader(urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, null) {

  def this(urls: Array[URL]) = this(urls, ClassLoader.getSystemClassLoader)

  @throws[ClassNotFoundException]
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    // This will actually load all Closure internals with the Closure Loader
    if (name.startsWith("com.google") || name.startsWith("javax.annotation") || name.startsWith("org.kohsuke.args4j")) {
      // println(s"INTERNAL: $name")
      super.loadClass(name, resolve)
    } else {
      // println(s"PARENT: $name")
      parent.loadClass(name)
    }
  }

}
