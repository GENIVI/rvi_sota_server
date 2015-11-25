/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.api.{Validate, Refined}
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


  val genName: Gen[String] =
    for {
      // We don't want name clashes so keep the names long.
      n  <- Gen.choose(20, 50)
      cs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield cs.mkString

  val genFilter: Gen[Filter] =
    for {
      name <- genName
      expr <- genFilterAST
    } yield Filter(Refined.unsafeApply(name), Refined.unsafeApply(ppFilter(expr)))

  implicit lazy val arbFilter = Arbitrary(genFilter)
}
