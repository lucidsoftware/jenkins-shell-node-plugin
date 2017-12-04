package com.lucidchart.jenkins.shellnode

import resource.makeManagedResource

object ProcessUtil {
  def run(builder: ProcessBuilder) =
    makeManagedResource(builder.start()) { process =>
      process.destroyForcibly()
      process.waitFor()
    }(Nil)
}
