/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.api.{Validate, Refined}
import org.genivi.sota.datatype.SemanticVin.genVinRegex
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

  val genName: Gen[String] =
    for {
      // We don't want name clashes so keep the names long.
      n  <- Gen.choose(20, 50)
      cs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield cs.mkString

  // These filters will be random and quite big, most likely never
  // letting any vehicles through.
  val genFilterViaAST: Gen[Filter] =
    for {
      name <- genName
      expr <- genFilterAST
    } yield Filter(Refined.unsafeApply(name), Refined.unsafeApply(ppFilter(expr)))

  implicit val arbFilter: Arbitrary[Filter] =
    Arbitrary(genFilterViaAST)

  def genFilter(pkgs: List[Package], comps: List[Component]): Gen[Filter] = {

    def helper(depth: Int): Gen[FilterAST] =
      depth match {
        case 0 => genLeaf
        case _ =>
          for {
            l    <- helper(depth - 1)
            r    <- helper(depth - 1)
            node <- Gen.frequency(
                      (10, Or),
                      (10, And),
                      (1, ((l: FilterAST, r: FilterAST) => Or(Not(l),  r))),
                      (1, ((l: FilterAST, r: FilterAST) => Or(l,       Not(r)))),
                      (1, ((l: FilterAST, r: FilterAST) => And(Not(l), r))),
                      (1, ((l: FilterAST, r: FilterAST) => And(l,      Not(r))))
                    )
          } yield node(l, r)
      }

    def genLeaf: Gen[FilterAST] =
      for {
        leaf <- Gen.frequency(
                  (3, genVinRegex.map(VinMatches(_))),
                  (if (pkgs.length == 0) 0 else 1,
                    Gen.choose(0, pkgs.length - 1)
                       .map(i => HasPackage(Refined.unsafeApply(pkgs(i).id.name.get),
                                            Refined.unsafeApply(pkgs(i).id.version.get)))),
                  (if (comps.length == 0) 0 else 1,
                    Gen.choose(0, comps.length - 1)
                       .map(i => HasComponent(Refined.unsafeApply(comps(i).partNumber.get))))
                )
      } yield leaf

    for {
      name  <- genName
      depth <- Gen.choose(0, 3)
      expr  <- Gen.frequency(
                 (100, helper(depth)),
                 (1,   genFilterAST)
               )
    } yield Filter(Refined.unsafeApply(name), Refined.unsafeApply(ppFilter(expr)))

  }

}
