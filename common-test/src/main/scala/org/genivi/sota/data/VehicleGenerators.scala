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

  val genVehicle: Gen[Vehicle] =
    genVin.map(Vehicle(_))

  implicit lazy val arbVehicle: Arbitrary[Vehicle] =
    Arbitrary(genVehicle)

  val genInvalidVehicle: Gen[Vehicle] =
    genInvalidVin.map(Vehicle(_))



}
