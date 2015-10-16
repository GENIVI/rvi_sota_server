/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.{Predicate, Refined}
import org.genivi.sota.resolver.filters.FilterAST.parseFilter


case class Filter(
  name: Filter.Name,
  expression: Filter.Expression
)

object Filter {

  trait ValidName
  trait ValidExpression

  type Name       = Refined[String, ValidName]
  type Expression = Refined[String, ValidExpression]

  case class ExpressionWrapper (
    expression: Filter.Expression
  )

  implicit val validFilterName: Predicate[ValidName, String] =
    Predicate.instance (name =>
      name.length > 1 && name.length <= 100 &&
        name.forall(c => c.isLetter || c.isDigit),
      name => s"($name should be between two and a hundred alphanumeric characters long.)")

  implicit val validFilterExpression: Predicate[ValidExpression, String] =
    Predicate.instance (expr => parseFilter(expr).isRight,
      expr => parseFilter(expr) match {
        case Left(e)  => s"($expr failed to parse: $e.)"
        case Right(_) => "IMPOSSIBLE"
    })
}
