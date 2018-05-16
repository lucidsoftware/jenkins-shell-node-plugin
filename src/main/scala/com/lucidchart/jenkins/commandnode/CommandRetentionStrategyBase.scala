package com.lucidchart.jenkins.commandnode

import hudson.model.{Queue, Descriptor => HudsonDescriptor}
import hudson.slaves.{OfflineCause, RetentionStrategy, SlaveComputer}
import java.io._
import java.time.{Duration, Instant}
import jenkins.model.Jenkins
import jenkins.util.NonLocalizable
import resource.managed
import scala.beans.BeanProperty
import scala.util.control.NonFatal

sealed trait Computer

class CommandRetentionStrategyBase(@BeanProperty val command: String)
    extends RetentionStrategy[SlaveComputer]
    with JavaLogging {
  def check(computer: SlaveComputer) = apply(computer, false)

  override def start(computer: SlaveComputer) = Queue.withLock(() => apply(computer, true))

  private[this] def apply(computer: SlaveComputer, start: Boolean) =
    try {
      logger.info(s"Checking retention for ${computer.getName}")
      val reader = for {
        process <- run(computer, start)
        input <- managed(process.getInputStream)
      } yield new BufferedReader(new InputStreamReader(input))
      reader.acquireAndGet { reader =>
        val nextCheck = reader.readLine().toLong
        Iterator.continually(reader.readLine()).takeWhile(_ != null).filter(_.nonEmpty).foreach { line =>
          line.split("\t", 2) match {
            case Array("ACCEPT")             => computer.setAcceptingTasks(true)
            case Array("CONNECT")            => computer.connect(true)
            case Array("DELETE")             => if (computer.getNode != null) { Jenkins.getInstance.removeNode(computer.getNode) }
            case Array("DISCONNECT")         => computer.disconnect()
            case Array("DISCONNECT", reason) => computer.disconnect(OfflineCause.create(new NonLocalizable(reason)))
            case Array("DISABLE")            => computer.setTemporarilyOffline(true)
            case Array("DISABLE", reason) =>
              computer.setTemporarilyOffline(true, OfflineCause.create(new NonLocalizable(reason)))
            case Array("ENABLE") => computer.setTemporarilyOffline(false)
            case Array("REJECT") => computer.setAcceptingTasks(false)
            case _               => logger.warning(s"""Unknown action: "$line"""")
          }
        }
        Duration.ofMillis(nextCheck).toMinutes
      }
    } catch {
      case NonFatal(e) =>
        logger.severe(() => {
          val writer = new StringWriter
          managed(new PrintWriter(writer)).foreach(e.printStackTrace)
          writer.toString
        })
        1
    }

  private[this] def run(computer: SlaveComputer, start: Boolean) = ProcessUtil.runShellScript(command) { builder =>
    builder.redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment.put("JENKINS_URL", Jenkins.getInstance.getRootUrl)
    builder.environment.put("NODE_ACCEPTING", computer.isAcceptingTasks.toString)
    builder.environment.put("NODE_CONNECTED", (computer.getChannel != null).toString)
    builder.environment.put("NODE_CONNECTING", computer.isConnecting.toString)
    builder.environment.put("NODE_DISABLED", computer.isTemporarilyOffline.toString)
    builder.environment.put("NODE_IDLE", computer.isIdle.toString)
    builder.environment.put(
      "NODE_IDLE_DURATION",
      Duration.between(Instant.ofEpochMilli(computer.getIdleStartMilliseconds), Instant.now).toMillis.toString
    )
    builder.environment.put("NODE_START", start.toString)
    builder.environment.put("NODE_NAME", computer.getName)
    Option(computer.getOfflineCause).foreach { cause =>
      builder.environment.put("NODE_OFFLINE_TIME", cause.getTimestamp.toString)
      builder.environment.put("NODE_OFFLINE_REASON", cause.toString)
    }
  }
}

object CommandRetentionStrategyBase {
  class Descriptor extends HudsonDescriptor[RetentionStrategy[_]] {
    override def getDisplayName = "Command"
  }
}
