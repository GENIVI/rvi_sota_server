/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import eu.timepit.refined.api.{Refined, Validate}
import org.genivi.sota.data.Namespace


case class Component(
  namespace: Namespace,
  partNumber: Component.PartNumber,
  description: String
) {
  def samePK(that: Component): Boolean = { (namespace == that.namespace) && (partNumber == that.partNumber) }
  override def toString: String = { s"Component(${partNumber.value}, $description)" }
}

object Component {

  case class ValidPartNumber()

  type PartNumber = Refined[String, ValidPartNumber]

  case class DescriptionWrapper(description: String)

  implicit val validPartNumber: Validate.Plain[String, ValidPartNumber] =
    Validate.fromPredicate(
      part => part.length > 0 && part.length <= 30 && part.forall(_.isLetterOrDigit),
      part => s"($part isn't a 30 character or shorter alpha numeric non-empty string)",
      ValidPartNumber()
    )

  implicit val PartNumberOrdering: Ordering[Component.PartNumber] = new Ordering[Component.PartNumber] {
    override def compare(part1: Component.PartNumber, part2: Component.PartNumber): Int =
      part1.value compare part2.value
  }

}
