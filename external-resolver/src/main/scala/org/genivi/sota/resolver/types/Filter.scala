/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.{Predicate, Refined}
import org.genivi.sota.resolver.types.FilterParser.parseFilter


case class Filter ( name: Filter.Name, expression: Filter.Expression )

object Filter {

  trait ValidName
  trait ValidExpression

  type Name       = Refined[String, ValidName]
  type Expression = Refined[String, ValidExpression]

  case class ExpressionWrapper (
    expression: Filter.Expression
  )

  import io.circe.generic.semiauto._
  import org.genivi.sota.marshalling.CirceInstances._

  object ExpressionWrapper {

    implicit val encoderInstance = deriveFor[ExpressionWrapper].encoder
    implicit val decoderInstance = deriveFor[ExpressionWrapper].decoder
  }

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

  implicit val encoderInstance = deriveFor[Filter].encoder
  implicit val decoderInstance = deriveFor[Filter].decoder
}