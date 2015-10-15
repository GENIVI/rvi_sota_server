/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.genivi.sota.datatype.VehicleCommon
import org.genivi.sota.rest.ErrorCode


case class Vehicle(
  vin: Vehicle.Vin
)

object Vehicle extends VehicleCommon {

  val MissingVehicle = new ErrorCode("missing_vehicle")

}
