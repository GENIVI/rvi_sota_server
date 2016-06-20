/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.data

import java.time.Instant

import org.genivi.sota.data.Namespace._
import Device._
import eu.timepit.refined.api.Refined

case class BlockedInstall(id: Id, blockedAt: Instant)

object BlockedInstall {
  def from(id: Id): BlockedInstall = BlockedInstall(id, Instant.now())
  def from(id: DeviceId): BlockedInstall = from(Device.Id(Refined.unsafeApply(id.underlying)))
}