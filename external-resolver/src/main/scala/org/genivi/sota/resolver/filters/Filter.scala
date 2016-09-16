/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import org.genivi.sota.data.Namespace._
import eu.timepit.refined.api.{Refined, Validate}
import org.genivi.sota.data.Namespace
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.db.Package
import org.genivi.sota.resolver.filters.FilterAST._
import org.scalacheck._


case class Filter(
  namespace: Namespace,
  name: Filter.Name,
  expression: Filter.Expression
) {
  def samePK(that: Filter): Boolean = { (namespace == that.namespace) && (name == that.name) }
  override def toString(): String = { s"Filter(${name.get}, ${expression.get})" }
}

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
