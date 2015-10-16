/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import org.genivi.sota.datatype.VehicleCommon


case class Vehicle(
  vin: Vehicle.Vin
)

object Vehicle extends VehicleCommon
