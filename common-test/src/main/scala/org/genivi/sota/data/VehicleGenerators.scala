/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import org.scalacheck.{Arbitrary, Gen}

/**
  * Created by vladimir on 16/03/16.
  */
object VehicleGenerators extends VinGenerators {

  implicit val VehicleOrdering: Ordering[Vehicle] =
    new Ordering[Vehicle] {
      override def compare(veh1: Vehicle, veh2: Vehicle): Int =
        veh1.vin.get compare veh2.vin.get
    }

  val genVehicle: Gen[Vehicle] = for {
    vin <- genVin
  } yield Vehicle(Namespaces.defaultNs, vin)

  implicit lazy val arbVehicle: Arbitrary[Vehicle] =
    Arbitrary(genVehicle)

  val genInvalidVehicle: Gen[Vehicle] = for {
    // TODO: for now, just generate an invalid VIN with a valid namespace
    vin <- genInvalidVin
  } yield Vehicle(Namespaces.defaultNs, vin)

}
