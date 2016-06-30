sbt-closure
===========

[sbt-web] plugin which integrates with [Google’s Closure Compiler].

This compiler is a fork of:

https://github.com/ground5hark/sbt-closure#sbt-closure
and is heavily influenced by
https://github.com/irundaia/sbt-sassify

**It works works totally different than sbt-closure.**

Currently this is in a really early stage, so stay tuned.

This will use reflection to load closure so that the usage of Scala.JS stays unaffected.
Currently this plugin will use newer Versions of Closure and still works just fine with Scala.JS (as of Version 0.5).

compiler-*.jar is just the downloaded the downloaded JAR from https://dl.google.com/closure-compiler/compiler-latest.zip
and renamed with the version number. So that we know which version is in use.

### Testing

sbt clean web-assets:assets