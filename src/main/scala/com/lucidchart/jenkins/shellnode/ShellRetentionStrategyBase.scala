package com.lucidchart.jenkins.shellnode

import hudson.model.{Descriptor => HudsonDescriptor}
import hudson.slaves.{RetentionStrategy, SlaveComputer}
import java.io.{BufferedReader, InputStreamReader}
import java.time.{Duration, Instant}
import jenkins.model.Jenkins
import resource.managed
import scala.beans.BeanProperty
import scala.util.control.NonFatal

class ShellRetentionStrategyBase(@BeanProperty val command: String)
    extends RetentionStrategy[SlaveComputer]
    with JavaLogging {
  def check(computer: SlaveComputer) =
    try {
      logger.info(s"Checking retention for ${computer.getName}")
      val reader = for {
        process <- run(computer)
        input <- managed(process.getInputStream)
      } yield new BufferedReader(new InputStreamReader(input))
      reader.acquireAndGet { reader =>
        val nextCheck = reader.readLine().toLong
        Option(reader.readLine()).filter(_.nonEmpty).map(ComputerStatus.parse).foreach(ComputerStatus.set(computer, _))
        Duration.ofMillis(nextCheck).toMinutes
      }
    } catch {
      case NonFatal(e) =>
        e.printStackTrace(System.err)
        1
    }

  private[this] def run(computer: SlaveComputer) = ProcessUtil.runShellScript(command) { builder =>
    builder.environment.put("JENKINS_URL", Jenkins.getInstance.getRootUrl)
    builder.environment.put("NODE_IDLE", computer.isIdle.toString)
    builder.environment.put(
      "NODE_IDLE_DURATION",
      Duration.between(Instant.ofEpochMilli(computer.getIdleStartMilliseconds), Instant.now).toMillis.toString
    )
    builder.environment.put("NODE_NAME", computer.getName)
    builder.environment.put("NODE_STATUS", ComputerStatus.serialize(ComputerStatus.get(computer)))
  }
}

object ShellRetentionStrategyBase {
  class Descriptor extends HudsonDescriptor[RetentionStrategy[_]] {
    override def getDisplayName = "Shell"
  }
}
