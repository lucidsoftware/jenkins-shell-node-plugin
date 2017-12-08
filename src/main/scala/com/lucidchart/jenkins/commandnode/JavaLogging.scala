package com.lucidchart.jenkins.commandnode

import java.util.logging.Logger

trait JavaLogging {
  @transient
  protected[this] lazy val logger = Logger.getLogger(getClass.getName)
}
