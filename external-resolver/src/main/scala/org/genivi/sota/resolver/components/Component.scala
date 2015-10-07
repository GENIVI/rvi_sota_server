/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import eu.timepit.refined.{Refined, Predicate}


case class Component(
  partNumber : Component.PartNumber,
  description: String
)

object Component {

  trait ValidPartNumber

  type PartNumber = Refined[String, ValidPartNumber]

  case class DescriptionWrapper(description: String)

  implicit val validPartNumber: Predicate[ValidPartNumber, String] =
    Predicate.instance(
      part => part.length <= 30 && part.forall(c => c.isLetter || c.isDigit),
      part => s"($part isn't a 30 character or shorter alpha numeric string)"
    )

}
