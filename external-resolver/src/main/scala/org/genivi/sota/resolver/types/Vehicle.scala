/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.{Refined, Predicate}
import org.genivi.sota.rest.ErrorCode


case class Vehicle(vin: Vehicle.Vin)

object Vehicle {

  trait ValidVin

  implicit val validVin : Predicate[ValidVin, String] = Predicate.instance(
    vin => vin.length == 17 && vin.forall(c => c.isLetter || c.isDigit),
    vin => s"($vin isn't 17 letters or digits long)"
  )

  type Vin = Refined[String, ValidVin]

  implicit val VinOrdering: Ordering[Vin] = new Ordering[Vin] {
    override def compare(a: Vin, b: Vin): Int = a.get compare b.get
  }

  val MissingVehicle = new ErrorCode("missing_vehicle")

}
