/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.common

import org.genivi.sota.data.PackageId
import org.genivi.sota.resolver.data.Firmware

case class InstalledSoftware(
  packages: Set[PackageId],
  firmware: Set[Firmware]
)

