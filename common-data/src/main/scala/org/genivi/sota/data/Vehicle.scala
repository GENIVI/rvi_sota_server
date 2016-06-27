/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import org.genivi.sota.data.Namespace._
import eu.timepit.refined.api.{Refined, Validate}
import java.time.Instant

/*
 * The notion of vehicle has a identification number (VIN), this is
 * shared between the core and resolver.
 */
case class Vehicle(namespace: Namespace, vin: Vehicle.Vin, lastSeen: Option[Instant] = None) {
  override def toString(): String = s"Vehicle(${vin.get}, $lastSeen)"
}

object Vehicle {
  def tupled: ((Namespace, Vin, Option[Instant])) => Vehicle = { case (ns, vin, lastSeen) =>
    Vehicle(ns, vin, lastSeen)
  }

  def fromVin: (((Namespace, Vin)) => Vehicle) = { case (ns, vin) => Vehicle(ns, vin, None) }

  def toVin: Vehicle => Option[(Namespace, Vin)] = { v => Some((v.namespace, v.vin)) }

  case class ValidVin()

  /**
    * A valid VIN, see ISO 3779 and ISO 3780, must be 17 letters or
    * digits long and not contain 'I', 'O' or 'Q'. We enforce this at the
    * type level by refining the string type with the following
    * predicate.
    *
    * @see [[https://github.com/fthomas/refined]]
    */
  implicit val validVin : Validate.Plain[String, ValidVin] = Validate.fromPredicate(
    vin => vin.length == 17
        && vin.forall(c => (c.isUpper  || c.isDigit)
        && (c.isLetter || c.isDigit)
        && !List('I', 'O', 'Q').contains(c)),
    vin => s"($vin must be 17 letters or digits long and not contain 'I', 'O', or 'Q')",
    ValidVin()
  )

  type Vin = Refined[String, ValidVin]

  implicit val VinOrdering: Ordering[Vin] = new Ordering[Vin] {
    override def compare(v1: Vin, v2: Vin): Int = v1.get compare v2.get
  }
}
