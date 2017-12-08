package com.lucidchart.jenkins.commandnode

import hudson.slaves.{OfflineCause, SlaveComputer}
import jenkins.model.Jenkins
import jenkins.util.NonLocalizable

sealed trait ComputerStatus

object ComputerStatus {
  object Deleted extends ComputerStatus
  object Disabled extends ComputerStatus
  object Enabled extends ComputerStatus
  case class Offline(cause: String) extends ComputerStatus

  def parse(value: String) = value.split("\\s+", 2).toList match {
    case "DELETED" :: _          => Deleted
    case "DISABLED" :: _         => Disabled
    case "ENABLED" :: _          => Enabled
    case "OFFLINE" :: value :: _ => Offline(value)
    case "OFFLINE" :: _          => Offline("")
  }

  def serialize(status: ComputerStatus) = status match {
    case Deleted        => "DELETED"
    case Disabled       => "DISABLED"
    case Enabled        => "ENABLED"
    case Offline(value) => s"OFFLINE\t$value"
  }

  def get(computer: SlaveComputer) =
    if (computer.isOffline) {
      Offline(computer.getOfflineCauseReason)
    } else if (computer.isAcceptingTasks) {
      Enabled
    } else {
      Disabled
    }

  def set(computer: SlaveComputer, status: ComputerStatus) = status match {
    case Deleted =>
      Jenkins.getInstance.removeNode(computer.getNode)
    case Disabled =>
      computer.connect(false)
      computer.setAcceptingTasks(false)
    case Enabled =>
      computer.connect(false)
      computer.setAcceptingTasks(true)
    case Offline(cause) => computer.disconnect(OfflineCause.create(new NonLocalizable(cause)))
  }

}
