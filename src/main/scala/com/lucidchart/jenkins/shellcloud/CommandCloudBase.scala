package com.lucidchart.jenkins.shellcloud

import com.lucidchart.jenkins.shellcloud.FutureUtil.promiseResource
import hudson.model.{Label, Node}
import hudson.slaves.Cloud
import hudson.slaves.NodeProvisioner.PlannedNode
import hudson.util.XStream2
import java.io.{BufferedReader, InputStreamReader}
import java.util.concurrent.CompletableFuture
import jenkins.model.Jenkins
import org.kohsuke.stapler.HttpResponses
import resource.{managed, ManagedResource}
import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

case class ProvisionParams(label: Label, workload: Int)

abstract class CommandCloudBase(
  name: String,
  @BeanProperty val labelString: String
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
      CommandCloudBase.XStream.fromXML(reader)
    }
    val node = nodeResource.acquireAndGet(_.asInstanceOf[Node])
    Jenkins.getInstance.addNode(node)
    HttpResponses.redirectViaContextPath(s"/computer/${node.getNodeName}")
  }

  private[this] def labels = labelString.split("\\s")

  def canProvision(label: Label) = labels.contains(label.getName)

  def provision(label: Label, workload: Int) = {
    @tailrec
    def next(nodes: List[PlannedNode], workload: Int): List[PlannedNode] =
      if (workload <= 0) {
        nodes
      } else {
        val node = planned(ProvisionParams(label, workload))
        next(node :: nodes, workload - node.numExecutors)
      }
    asJavaCollection(next(Nil, workload))
  }

  private[this] def planned(params: ProvisionParams) = {
    val promise = Promise[PlannedNode]()
    new Thread(() => {
      for {
        promise <- managed(promise)
        process <- run(Some(params))
        input <- managed(process.getInputStream)
      } {
        process.getOutputStream.close()
        logger.info(s"Provisioning node for ${params.label}")
        val reader = new BufferedReader(new InputStreamReader(input))
        val line = reader.readLine()
        val (name, capacity) = line.split(" ") match { case Array(name, capacity) => name -> capacity.toInt }
        val future = new CompletableFuture[Node]
        logger.info(s"Planned node $name for ${params.label}")
        promise.success(new PlannedNode(name, future, capacity) { override def spent() = process.destroy() })
        FutureUtil.completeWith(future)(CommandCloudBase.XStream.fromXML(reader).asInstanceOf[Node])
        logger.info(s"Provisioned ${future.get.getNodeName} for ${params.label}")
      }
    })
    Await.result(promise.future, Duration.Inf)
  }

  def run(params: Option[ProvisionParams]): ManagedResource[Process]
}

object CommandCloudBase {
  private val XStream = new XStream2
}
