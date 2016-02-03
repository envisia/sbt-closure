package de.envisia.sbt.closure


final class SbtClosureClassLoader(closureLoader: ClassLoader, parent: ClassLoader)
  extends ClassLoader(parent) {

  def this(loader: ClassLoader) = this(loader, ClassLoader.getSystemClassLoader)

  override protected def loadClass(name: String, resolve: Boolean): Class[_] = {
    try {
      closureLoader.loadClass(name)
    } catch {
      case _: ClassNotFoundException => super.loadClass(name, resolve)
    }
  }

}
