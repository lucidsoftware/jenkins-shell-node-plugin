package com.lucidchart.jenkins.commandnode

import java.nio.file.Files
import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import resource.{makeManagedResource, DefaultManagedResource, ManagedResource}
import scala.collection.JavaConverters._

object ProcessUtil {
  def checkSuccess(process: Process): DefaultManagedResource[Unit] =
    makeManagedResource(())(
      _ =>
        process.waitFor() match {
          case 0 =>
          case x => throw new RuntimeException(s"Command failed with exit code $x")
      }
    )(Nil)

  def run(builder: ProcessBuilder): ManagedResource[Process] =
    makeManagedResource(builder.start()) { process =>
      process.destroyForcibly()
      process.waitFor()
    }(Nil)

  def runShellScript(command: String)(f: ProcessBuilder => Unit): ManagedResource[Process] =
    for {
      file <- FileUtil.managedFile(
        Files.createTempFile(
          "cloud-",
          ".sh",
          PosixFilePermissions.asFileAttribute(
            Set(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE).asJava
          )
        )
      )
      process <- ProcessUtil.run {
        Files.write(file, command.getBytes)
        val builder = new ProcessBuilder().command(file.toString)
        f(builder)
        builder
      }
      _ <- checkSuccess(process)
    } yield process
}
