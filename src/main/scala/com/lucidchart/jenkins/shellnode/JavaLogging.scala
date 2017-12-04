package com.lucidchart.jenkins.shellnode

import java.util.logging.Logger

trait JavaLogging {
  @transient
  protected[this] lazy val logger = Logger.getLogger(getClass.getName)
}
