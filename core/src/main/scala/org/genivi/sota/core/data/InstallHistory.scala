/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.joda.time.DateTime

/**
 * Domain object for the install history of a VIN
 * @param id The Id in the database. Initialize to Option.None
 * @param vin The VIN that this install history belongs to
 * @param packageId The package name/version that was attempted to be installed
 * @param success The outcome of the install attempt
 * @param completionTime The date the install was attempted
 */
case class InstallHistory(
  id            : Option[Long],
  vin           : Vehicle.Vin,
  packageId     : Package.Id,
  success       : Boolean,
  completionTime: DateTime
)
