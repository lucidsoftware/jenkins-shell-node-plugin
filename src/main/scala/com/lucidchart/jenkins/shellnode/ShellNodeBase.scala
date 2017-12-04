package com.lucidchart.jenkins.shellnode

import hudson.model.{ComputerSet, Slave}
import hudson.slaves.NodeDescriptor
import jenkins.model.Jenkins
import org.kohsuke.stapler.{StaplerRequest, StaplerResponse}
import scala.collection.JavaConverters._

object ShellNodeBase {

  class Descriptor extends NodeDescriptor(classOf[Slave]) {
    override def getDisplayName = "Shell Cloud"

    override def handleNewNodePage(
      computerSet: ComputerSet,
      name: String,
      req: StaplerRequest,
      rsp: StaplerResponse
    ) = {
      computerSet.checkName(name)
      req.setAttribute("descriptor", this)
      req.getView(getClass.getEnclosingClass, "_new.jelly").forward(req, rsp)
    }

    def getClouds = Jenkins.getInstance.clouds.asScala.collect { case cloud: ShellCloudBase => cloud.name }.asJava
  }

}
