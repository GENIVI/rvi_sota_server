/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import org.genivi.sota.rest.Validation

case class FilterId(id: Long)

case class Filter (
  id        : Option[FilterId],
  name      : String,
  expression: String
)

object Filter {

  import spray.json.DefaultJsonProtocol._
  import eu.timepit.refined.Predicate
  import Validation.Valid
  import shapeless.tag._
  import org.genivi.sota.resolver.types.FilterParser.parseFilter


  implicit val filterIdFormat   = jsonFormat1(FilterId.apply)
  implicit val filterFormat     = jsonFormat3(Filter.apply)
  implicit val filterListFormat = seqFormat[Filter]

  type ValidFilter = Filter @@ Valid

  implicit val validFilter: Predicate[Valid, Filter] =
    Predicate.instance(filter => filter.name.nonEmpty && parseFilter(filter.expression).isRight,
                       filter => parseFilter(filter.expression) match {
                         case Left(e)  => e
                         case Right(_) => s"(${filter} has no name)"
                       })
}
