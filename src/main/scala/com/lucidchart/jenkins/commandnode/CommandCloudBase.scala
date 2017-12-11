package com.lucidchart.jenkins.commandnode

import hudson.model.labels.LabelAtom
import hudson.model.{Label, Node, Descriptor => HudsonDescriptor}
import hudson.slaves.Cloud
import hudson.slaves.NodeProvisioner.PlannedNode
import java.io.{BufferedReader, InputStreamReader, PrintStream}
import java.util.concurrent.CompletableFuture
import jenkins.model.Jenkins
import org.apache.commons.io.IOUtils
import org.kohsuke.stapler.HttpResponses
import resource.managed
import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.control.NonFatal

case class ProvisionParams(label: Option[Label], workload: Int)

class ShellCloudBase(
  @BeanProperty val command: String,
  name: String,
  @BeanProperty val labelString: String
) extends Cloud(name)
    with JavaLogging {

  def doProvision() = {
    checkPermission(Cloud.PROVISION)
    var error: String = null
    val nodeResource = try {
       for {
        process <- run(None, ProcessBuilder.Redirect.PIPE)
        input <- managed(process.getInputStream)
      } yield {
        process.getOutputStream.close()
        val errorThread = new Thread(() => error = IOUtils.toString(process.getErrorStream))
        errorThread.run()
        val reader = new BufferedReader(new InputStreamReader(input))
        reader.readLine()
        CustomNodeBase.XStream.fromXML(reader)
      }
    } catch {
      case NonFatal(e) if error != null => throw new RuntimeException(s"Failed to provision:\n$error", e)
    }
    val node = nodeResource.acquireAndGet(_.asInstanceOf[Node])
    Jenkins.getInstance.addNode(node)
    HttpResponses.redirectViaContextPath(s"/computer/${node.getNodeName}")
  }

  private[this] def labels = labelString.split("\\s+").map(new LabelAtom(_)).toSeq

  def canProvision(label: Label) = label.matches(labels.asJavaCollection)

  def provision(label: Label, workload: Int) = {
    @tailrec
    def next(nodes: List[PlannedNode], workload: Int): List[PlannedNode] =
      if (workload <= 0) {
        nodes
      } else {
        planned(ProvisionParams(Option(label), workload)) match {
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
            for {
              output <- managed(process.getOutputStream)
              output <- managed(new PrintStream(output))
            } {
              params.label
                .fold(Jenkins.getInstance.unlabeledNodeProvisioner)(_.nodeProvisioner)
                .getPendingLaunches
                .forEach(node => output.print(s"${node.displayName}\t${node.numExecutors}"))
            }
            logger.info(s"Provisioning node for ${params.label.getOrElse("-")}")
            val reader = new BufferedReader(new InputStreamReader(input))
            val line = reader.readLine()
            if (line == "-") {
              promise.success(None)
            } else {
              val (name, capacity) = line.split("\t") match {
                case Array(name, capacity) => name -> capacity.toInt
              }
              val future = new CompletableFuture[Node]
              logger.info(s"Planned node $name for ${params.label.getOrElse("-")}")
              promise.success(Some(new PlannedNode(name, future, capacity) {
                override def spent() = process.destroy()
              }))
              FutureUtil.completeWith(future)(CustomNodeBase.XStream.fromXML(reader).asInstanceOf[Node])
              logger.info(s"Provisioned ${future.get.getNodeName} for ${params.label.getOrElse("-")}")
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

  private[this] def run(params: Option[ProvisionParams], error: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT) = ProcessUtil.runShellScript(command) { builder =>    builder.environment.put("JENKINS_URL", Jenkins.getInstance.getRootUrl)
    builder.environment.put("CLOUD_NAME", getDisplayName)
    params.foreach {
      case ProvisionParams(label, workload) =>
        builder.environment.put("NODE_CAPACITY", workload.toString)
        label.foreach(label => builder.environment.put("NODE_LABEL", label.getName))
    }
  }
}

object ShellCloudBase {
  class Descriptor extends HudsonDescriptor[Cloud] {
    override def getDisplayName = "Command Cloud"
  }
}
