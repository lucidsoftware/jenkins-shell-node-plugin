package com.lucidchart.jenkins.commandnode

import java.nio.file.{Files, Path}
import resource.{makeManagedResource, ManagedResource}

object FileUtil {
  def managedFile(file: Path): ManagedResource[Path] = makeManagedResource(file)(_ => () /*Files.delete*/ )(Nil)
}
