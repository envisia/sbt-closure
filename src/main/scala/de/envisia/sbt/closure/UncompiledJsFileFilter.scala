package de.envisia.sbt.closure

import sbt._


class UncompiledJsFileFilter(suffix: String) extends FileFilter {
  override def accept(file: File): Boolean =
    // visible
    !HiddenFileFilter.accept(file) &&
    // not already compiled
    !file.getName.endsWith(suffix) &&
    // a JS file
    file.getName.endsWith(".js")
}
