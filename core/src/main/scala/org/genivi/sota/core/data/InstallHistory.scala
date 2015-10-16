/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.joda.time.DateTime


case class InstallHistory(
  id            : Option[Long],
  vin           : Vehicle.Vin,
  packageId     : Package.Id,
  success       : Boolean,
  completionTime: DateTime
)
