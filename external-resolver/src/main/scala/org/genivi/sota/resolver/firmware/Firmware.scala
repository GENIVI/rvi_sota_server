/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.data

import org.genivi.sota.datatype.FirmwareCommon

case class Firmware(
  module: Firmware.Module,
  firmwareId: Firmware.FirmwareId,
  lastModified: Long
)

object Firmware extends FirmwareCommon
