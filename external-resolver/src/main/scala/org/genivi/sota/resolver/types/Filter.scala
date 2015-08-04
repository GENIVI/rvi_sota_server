/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.Predicate
import org.genivi.sota.resolver.Validation.Valid
import shapeless.tag._

case class Filter (
  id: Option[Long],
  name: String,
  expression: String
)

object Filter {

  import spray.json.DefaultJsonProtocol._

  implicit val filterFormat = jsonFormat3(Filter.apply)


  type ValidFilter= Filter @@ Valid

  implicit val validFilter: Predicate[Valid, Filter] =
    Predicate.instance(filter => filter.name.nonEmpty && filter.expression.nonEmpty,
      filter => s"(${filter} has no name and/or filter expression)")
}
