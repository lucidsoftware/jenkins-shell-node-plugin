package com.lucidchart.jenkins.shellnode

import hudson.model.{Descriptor => HudsonDescriptor}
import hudson.slaves.{RetentionStrategy, SlaveComputer}
import java.time.{Duration, Instant}
import jenkins.model.Jenkins
import scala.beans.BeanProperty

class ShellRetentionStrategyBase(@BeanProperty val command: String) extends CommandRetentionStrategyBase {
  def run(computer: SlaveComputer) = ProcessUtil.runShellScript(command) { builder =>
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
