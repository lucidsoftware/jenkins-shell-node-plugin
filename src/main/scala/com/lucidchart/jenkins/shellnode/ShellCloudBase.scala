package com.lucidchart.jenkins.shellnode

import hudson.model.{Descriptor => HudsonDescriptor}
import hudson.slaves.Cloud
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

  def run(params: Option[ProvisionParams]) = ProcessUtil.runShellScript(command) { builder =>
    builder.environment.put("JENKINS_URL", Jenkins.getInstance.getRootUrl)
    builder.environment.put("CLOUD_NAME", name)
    params.foreach {
      case ProvisionParams(label, workload) =>
        builder.environment.put("NODE_CAPACITY", workload.toString)
        builder.environment.put("NODE_LABEL", label.getName)
    }
  }
}

object ShellCloudBase {
  class Descriptor extends HudsonDescriptor[Cloud] {
    override def getDisplayName = "Shell"
  }
}
