/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.{Refined, Predicate}
import io.circe.{Encoder, Decoder}
import io.circe.generic.auto._


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

  implicit def vehicleMapEncoder[V]
    (implicit valueEncoder: Encoder[V])
      : Encoder[Map[Vehicle.Vin, V]]
  = Encoder[Seq[(Vehicle.Vin, V)]].contramap((m: Map[Vehicle.Vin, V]) => m.toSeq)

  implicit def vehicleMapDecoder[V]
    (implicit valueDecoder: Decoder[V])
      : Decoder[Map[Vehicle.Vin, V]]
  = Decoder[Seq[(Vehicle.Vin, V)]].map(_.toMap)

}
