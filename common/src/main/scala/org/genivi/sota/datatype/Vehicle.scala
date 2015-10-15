/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import eu.timepit.refined.{Predicate, Refined}


trait VehicleCommon {

  trait ValidVin

  implicit val validVin : Predicate[ValidVin, String] = Predicate.instance(
    vin => vin.length == 17 && vin.forall(c => c.isLetter || c.isDigit),
    vin => s"($vin isn't 17 letters or digits long)"
  )

  type Vin = Refined[String, ValidVin]

  implicit val VinOrdering: Ordering[Vin] = new Ordering[Vin] {
    override def compare(v1: Vin, v2: Vin): Int = v1.get compare v2.get
  }

}
