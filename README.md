# Jenkins Shell Cloud Plugin

Plugin for shell-based nodes

## Description

Don't want to write a Jenkins plugin for provisioning slaves? Instead, call easily customizable scripts which do the
heavy lifting.

## Build

Install [sbt](http://www.scala-sbt.org/download.html).

Build the .hpi with `sbt hpi:packageBin`.

To build and install the plugin to a Jenkins instance at http://localhost:8080/, `sbt hpi:install`.

To build a minified version, replace `sbt` with `sbt -Dproguard=true`.
