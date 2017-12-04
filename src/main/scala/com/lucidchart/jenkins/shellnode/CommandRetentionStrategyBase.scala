package com.lucidchart.jenkins.shellnode

import hudson.slaves.{RetentionStrategy, SlaveComputer}
import java.io.{BufferedReader, InputStreamReader}
import java.time.Duration
import resource.{managed, ManagedResource}
import scala.util.control.NonFatal

abstract class CommandRetentionStrategyBase extends RetentionStrategy[SlaveComputer] with JavaLogging {
  def check(computer: SlaveComputer) =
    try {
      //logger.info(s"Checking retention for ${computer.getName}")
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

  def run(computer: SlaveComputer): ManagedResource[Process]
}
