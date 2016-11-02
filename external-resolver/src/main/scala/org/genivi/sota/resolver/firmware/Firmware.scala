/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.firmware

import org.genivi.sota.data.Namespace
import org.genivi.sota.datatype.FirmwareCommon


case class Firmware(
  namespace: Namespace,
  module: Firmware.Module,
  firmwareId: Firmware.FirmwareId,
  lastModified: Long
)

object Firmware extends FirmwareCommon
