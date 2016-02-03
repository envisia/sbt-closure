package de.envisia.sbt.closure


final class SbtClosureClassLoader(closureLoader: ClassLoader, parent: ClassLoader)
  extends ClassLoader(parent) {

  def this(loader: ClassLoader) = this(loader, ClassLoader.getSystemClassLoader)

  private val bridgeClasses = Set(
    "de.envisia.closure.SbtClosureCommandLineRunner",
    "de.envisia.sbt.closure.SbtClosureCommandLineRunner",
    "de.envisia.sbt.closure.SbtClosureCommandLineRunner$"
  )


  override protected def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (bridgeClasses.contains(name)) {
      Option(findLoadedClass(name)).getOrElse {
        val wsManager = parent.getResourceAsStream(name.replace('.', '/') + ".class")

        if (wsManager == null) {
          throw new ClassNotFoundException(name)
        } else {
          val buf = Stream.continually(wsManager.read).takeWhile(_ != -1).map(_.toByte).toArray
          defineClass(name, buf, 0, buf.length)
        }
      }
    } else {
      try {
        closureLoader.loadClass(name)
      } catch {
        case _: ClassNotFoundException => super.loadClass(name, resolve)
      }
    }
  }

}
