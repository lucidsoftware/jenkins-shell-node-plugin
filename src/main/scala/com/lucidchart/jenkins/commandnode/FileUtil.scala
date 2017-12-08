package com.lucidchart.jenkins.commandnode

import java.nio.file.{Files, Path}
import resource.makeManagedResource

object FileUtil {
  def managedFile(file: Path) = makeManagedResource(file)(_ => () /*Files.delete*/ )(Nil)
}
