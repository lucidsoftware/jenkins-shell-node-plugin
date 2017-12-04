package com.lucidchart.jenkins.shellnode

import java.util.logging.Logger

trait JavaLogging {
  @transient
  protected[this] val logger = Logger.getLogger(getClass.getName)
}
