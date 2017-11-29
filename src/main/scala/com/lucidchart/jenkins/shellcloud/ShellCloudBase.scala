package com.lucidchart.jenkins.shellcloud

import hudson.model.{Descriptor => HudsonDescriptor}
import hudson.slaves.Cloud
import java.nio.file.Files
import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import jenkins.model.Jenkins
import scala.beans.BeanProperty
import scala.collection.JavaConverters._

class ShellCloudBase(
  @BeanProperty val command: String,
  name: String,
  labelString: String
) extends CommandCloudBase(name, labelString) {
  def getClouds =
    Jenkins.getInstance.clouds.asScala
      .collect { case cloud: ShellCloudBase => cloud }
      .sortBy(_.name)
      .asJava

  def run(params: Option[ProvisionParams]) =
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
        val builder = new ProcessBuilder()
          .command(file.toString)
          .redirectError(ProcessBuilder.Redirect.INHERIT)
        builder.environment.put("JENKINS_URL", Jenkins.getInstance.getRootUrl)
        params.foreach {
          case ProvisionParams(label, workload) =>
            builder.environment.put("NODE_CAPACITY", workload.toString)
            builder.environment.put("NODE_LABEL", label.getName)
        }
        builder
      }
    } yield process
}

object ShellCloudBase {
  class Descriptor extends HudsonDescriptor[Cloud] {
    override def getDisplayName = "Shell"
  }
}
