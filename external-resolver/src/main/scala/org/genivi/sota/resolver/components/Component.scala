/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import eu.timepit.refined.api.{Refined, Validate}


case class Component(
  partNumber : Component.PartNumber,
  description: String
)

object Component {

  case class ValidPartNumber()

  type PartNumber = Refined[String, ValidPartNumber]

  case class DescriptionWrapper(description: String)

  implicit val validPartNumber: Validate.Plain[String, ValidPartNumber] =
    Validate.fromPredicate(
      part => part.length > 0 && part.length <= 30 && part.forall(c => c.isLetter || c.isDigit),
      part => s"($part isn't a 30 character or shorter alpha numeric non-empty string)",
      ValidPartNumber()
    )

  implicit val PartNumberOrdering: Ordering[Component.PartNumber] = new Ordering[Component.PartNumber] {
    override def compare(part1: Component.PartNumber, part2: Component.PartNumber): Int =
      part1.get compare part2.get
  }

}
