package com.lucidchart.jenkins.commandnode

import hudson.model.{Label, Node, Descriptor => HudsonDescriptor}
import hudson.slaves.Cloud
import hudson.slaves.NodeProvisioner.PlannedNode
import java.io.{BufferedReader, InputStreamReader}
import java.util.concurrent.CompletableFuture
import jenkins.model.Jenkins
import org.kohsuke.stapler.HttpResponses
import resource.managed
import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.control.NonFatal

case class ProvisionParams(label: Label, workload: Int)

class ShellCloudBase(
  @BeanProperty val command: String,
  name: String,
  @BeanProperty labelString: String
) extends Cloud(name)
    with JavaLogging {

  def doProvision() = {
    checkPermission(Cloud.PROVISION)
    val nodeResource = for {
      process <- run(None)
      input <- managed(process.getInputStream)
    } yield {
      val reader = new BufferedReader(new InputStreamReader(input))
      reader.readLine()
      CustomNodeBase.XStream.fromXML(reader)
    }
    val node = nodeResource.acquireAndGet(_.asInstanceOf[Node])
    Jenkins.getInstance.addNode(node)
    HttpResponses.redirectViaContextPath(s"/computer/${node.getNodeName}")
  }

  private[this] def labels = labelString.split("\\s+")

  def canProvision(label: Label) = labels.contains(label.getName)

  def provision(label: Label, workload: Int) = {
    println("provision(" + label.getName + ", " + workload + ")")
    @tailrec
    def next(nodes: List[PlannedNode], workload: Int): List[PlannedNode] =
      if (workload <= 0) {
        nodes
      } else {
        planned(ProvisionParams(label, workload)) match {
          case Some(node) => next(node :: nodes, workload - node.numExecutors)
          case None       => nodes
        }
      }
    asJavaCollection(next(Nil, workload))
  }

  private[this] def planned(params: ProvisionParams) = {
    val promise = Promise[Option[PlannedNode]]()
    val thread = new Thread(
      () =>
        try {
          for {
            process <- run(Some(params))
            input <- managed(process.getInputStream)
          } {
            process.getOutputStream.close()
            logger.info(s"Provisioning node for ${params.label}")
            val reader = new BufferedReader(new InputStreamReader(input))
            val line = reader.readLine()
            if (line == "0") {
              promise.success(None)
            } else {
              val (name, capacity) = line.split(" ") match {
                case Array(name, capacity) => name -> capacity.toInt
              }
              val future = new CompletableFuture[Node]
              logger.info(s"Planned node $name for ${params.label}")
              promise.success(Some(new PlannedNode(name, future, capacity) {
                override def spent() = process.destroy()
              }))
              FutureUtil.completeWith(future)(CustomNodeBase.XStream.fromXML(reader).asInstanceOf[Node])
              logger.info(s"Provisioned ${future.get.getNodeName} for ${params.label}")
            }
          }
        } catch {
          case NonFatal(e) => promise.tryFailure(e)
      }
    )
    thread.run()
    Await.result(promise.future, Duration.Inf)
  }

  def getClouds =
    Jenkins.getInstance.clouds.asScala
      .collect { case cloud: ShellCloudBase => cloud }
      .sortBy(_.name)
      .asJava

  private[this] def run(params: Option[ProvisionParams]) = ProcessUtil.runShellScript(command) { builder =>
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
    override def getDisplayName = "Command Cloud"
  }
}
