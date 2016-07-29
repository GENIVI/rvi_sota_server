/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.data

import java.time.Instant

import org.genivi.sota.data.Device
import eu.timepit.refined.api.Refined

case class BlockedInstall(id: Device.Id, blockedAt: Instant)

object BlockedInstall {
  def from(id: Device.Id): BlockedInstall = BlockedInstall(id, Instant.now())
}
