package com.lucidchart.jenkins.commandnode

import hudson.model.{ManagementLink, Node}
import hudson.util.XStream2
import jenkins.model.Jenkins
import org.kohsuke.stapler.{HttpResponse, HttpResponses}

class CustomNodeBase extends ManagementLink {
  override def getDescription = "Create a new node from XML"

  def getDisplayName = "Create custom node"

  def getIconFileName = "images/24x24/new-computer.png"

  def getUrlName = "custom-node"

  def doCreateNode(config: String): HttpResponse = {
    val jenkins = Jenkins.getInstance
    jenkins.checkPermission(Jenkins.ADMINISTER)
    val node = CustomNodeBase.XStream.fromXML(config).asInstanceOf[Node]
    jenkins.addNode(node)
    HttpResponses.redirectViaContextPath(s"/computer/${node.getNodeName}")
  }
}

object CustomNodeBase {
  val XStream = new XStream2
}
