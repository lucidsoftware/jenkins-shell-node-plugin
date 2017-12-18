# Jenkins Shell Cloud Plugin

Executable hooks for node management

## Description

Do you have a need for custom cloud and node management, but don't want to write a full-on Jenkins Java plugin? The
Command Node plugin delegates to external scripts and executables to do the heavy lifting.

## Build

Install [sbt](http://www.scala-sbt.org/download.html).

Build the .hpi with `sbt hpi:packageBin`.

To build and install the plugin to a Jenkins instance at http://localhost:8080/, `sbt hpi:install`.

To build a minified version, replace `sbt` with `sbt -Dproguard=true`.

## Features

### Command Cloud

Plan and add new nodes whenever Jenkins determines more are needed.

### Command Retention Strategy

Control a node's lifecycle based on its current state, how long it has been idle, etc.

### TODO: Command Launcher + Computer

Connect to nodes, and react to events like agent termination.
