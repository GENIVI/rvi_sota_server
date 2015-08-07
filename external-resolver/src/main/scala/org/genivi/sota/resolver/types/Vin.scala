/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import org.genivi.sota.rest.Validation

case class Vin(vin: String)

object Vin {
  import eu.timepit.refined._
  import Validation._
  import shapeless.tag.@@
  import spray.json.DefaultJsonProtocol._

  implicit val vinFormat = jsonFormat1(Vin.apply)
  implicit val vinListFormat = seqFormat[Vin]

  type ValidVin = Vin @@ Valid

  implicit val validVin: Predicate[Valid, Vin] =
    Predicate.instance(vin => vin.vin.length == 17 && vin.vin.forall(c => c.isLetter || c.isDigit),
                       vin => s"(${vin} isn't 17 letters or digits long)")
}
