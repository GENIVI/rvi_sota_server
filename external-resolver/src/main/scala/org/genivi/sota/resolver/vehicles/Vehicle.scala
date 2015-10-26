/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import org.genivi.sota.datatype.VehicleCommon
import org.scalacheck.{Arbitrary, Gen}


case class Vehicle(
  vin: Vehicle.Vin
)

object Vehicle extends VehicleCommon {

  val genVehicle: Gen[Vehicle] =
    genVin.map(Vehicle(_))

  implicit lazy val arbVehicle: Arbitrary[Vehicle] =
    Arbitrary(genVehicle)

  val genInvalidVehicle: Gen[Vehicle] =
    genInvalidVin.map(Vehicle(_))

}
