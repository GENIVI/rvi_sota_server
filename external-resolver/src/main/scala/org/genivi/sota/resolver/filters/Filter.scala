/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.api.{Validate, Refined}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.FilterAST._
import org.scalacheck._


case class Filter(
  name: Filter.Name,
  expression: Filter.Expression
)

object Filter {

  case class ValidName()
  case class ValidExpression()

  type Name       = Refined[String, ValidName]
  type Expression = Refined[String, ValidExpression]

  case class ExpressionWrapper (
    expression: Filter.Expression
  )

  implicit val validFilterName: Validate.Plain[String, ValidName] =
    Validate.fromPredicate(
      name => name.length > 1
           && name.length <= 100
           && name.forall(c => c.isLetter || c.isDigit),
      name => s"($name should be between two and a hundred alphanumeric characters long.)",
      ValidName()
    )

  implicit val validFilterExpression: Validate.Plain[String, ValidExpression] =
    Validate.fromPredicate(
      expr => parseFilter(expr).isRight,
      expr => parseFilter(expr) match {
        case Left(e)  => s"($expr failed to parse: $e.)"
        case Right(_) => "IMPOSSIBLE"
      },
      ValidExpression()
    )

}
