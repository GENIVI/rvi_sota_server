/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.common

import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.packages.Package

case class InstalledSoftware(
  packages: Set[Package.Id],
  firmware: Set[Firmware]
)

