/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import eu.timepit.refined.{Predicate, Refined}
import io.circe.{Encoder, Decoder}
import io.circe.generic.auto._

case class Vehicle(vin: Vehicle.IdentificationNumber)

object Vehicle {

  trait Vin
  implicit val validVin : Predicate[Vin, String] = Predicate.instance(
    vin => vin.length == 17 && vin.forall(c => c.isLetter || c.isDigit),
    vin => s"(${vin} isn't 17 letters or digits long)"
  )

  type IdentificationNumber = String Refined Vin

  implicit val VinOrdering : Ordering[IdentificationNumber] = new Ordering[IdentificationNumber] {
    override def compare( a: IdentificationNumber, b: IdentificationNumber ) : Int = a.get compare b.get
  }

}
