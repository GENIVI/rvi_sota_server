/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.genivi.sota.datatype.VehicleCommon
import org.genivi.sota.rest.ErrorCode
import org.scalacheck.{Arbitrary, Gen}

/**
 * Domain object for a vehicle.
 * Vehicles in SOTA are known by a VIN. It is assumed that the VIN is constant
 * over the life of a system that receives updates from SOTA.
 *
 * @param vin The VIN that uniquely identifies the vehicle
 */
case class Vehicle(
  vin: Vehicle.Vin
)

/**
 * Utility functions for creating VINs
 */
object Vehicle extends VehicleCommon {

  /**
   * An invalid vehicle
   */
  val MissingVehicle = new ErrorCode("missing_vehicle")

  /**
   * A random VIN, for scalacheck
   */
  val genVehicle: Gen[Vehicle] =
    genVin.map(Vehicle(_))

  implicit lazy val arbVehicle: Arbitrary[Vehicle] =
    Arbitrary(genVehicle)

  /**
   * A VIN that is invalid
   */
  val genInvalidVehicle: Gen[Vehicle] =
    genInvalidVin.map(Vehicle(_))

}
