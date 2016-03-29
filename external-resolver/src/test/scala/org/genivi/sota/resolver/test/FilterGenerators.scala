package org.genivi.sota.resolver.test

import eu.timepit.refined.api.Refined
import org.genivi.sota.data.SemanticVin
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters._
import org.genivi.sota.resolver.filters.FilterAST._
import org.genivi.sota.resolver.packages.Package
import org.scalacheck.{Arbitrary, Gen}

trait FilterGenerators  {

  def genFilterHelper(i: Int): Gen[FilterAST] = {

    def genNullary = Gen.oneOf(True, False)

    def genUnary(n: Int) = for {
      f     <- genFilterHelper(n / 2)
      unary <- Gen.oneOf(Not, Not)
    } yield unary(f)

    def genBinary(n: Int) = for {
      l      <- genFilterHelper(n / 2)
      r      <- genFilterHelper(n / 2)
      binary <- Gen.oneOf(Or, And)
    } yield binary(l, r)

    def genLeaf = Gen.oneOf(
      for {
        s <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
        leaf <- Gen.oneOf(VinMatches, HasComponent)
      } yield leaf(Refined.unsafeApply(s.mkString)),
      for {
        s <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
        t <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
      } yield HasPackage(Refined.unsafeApply(s.mkString), Refined.unsafeApply(t.mkString))
    )

    i match {
      case 0 => genLeaf
      case n => Gen.frequency(
        (2, genNullary),
        (8, genUnary(n)),
        (10, genBinary(n))
      )
    }
  }

  def genFilterAST: Gen[FilterAST] = Gen.sized(genFilterHelper)

  implicit lazy val arbFilterAST: Arbitrary[FilterAST] =
    Arbitrary(genFilterAST)

  val genFilterName: Gen[String] =
    for {
    // We don't want name clashes so keep the names long.
      n  <- Gen.choose(20, 50)
      cs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield cs.mkString

  // These filters will be random and quite big, most likely never
  // letting any vehicles through.
  val genFilterViaAST: Gen[Filter] =
    for {
      name <- genFilterName
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
              (1, (l: FilterAST, r: FilterAST) => Or(Not(l), r)),
              (1, (l: FilterAST, r: FilterAST) => Or(l, Not(r))),
              (1, (l: FilterAST, r: FilterAST) => And(Not(l), r)),
              (1, (l: FilterAST, r: FilterAST) => And(l, Not(r)))
            )
          } yield node(l, r)
      }

    def genLeaf: Gen[FilterAST] =
      for {
        leaf <- Gen.frequency(
          (3, SemanticVin.genVinRegex.map(VinMatches(_))),
          (if (pkgs.isEmpty) 0 else 1,
              Gen.choose(0, pkgs.length - 1)
                  .map(i => HasPackage(Refined.unsafeApply(pkgs(i).id.name.get),
                    Refined.unsafeApply(pkgs(i).id.version.get)))),
          (if (comps.isEmpty) 0 else 1,
              Gen.choose(0, comps.length - 1)
                  .map(i => HasComponent(Refined.unsafeApply(comps(i).partNumber.get))))
        )
      } yield leaf

    for {
      name  <- genFilterName
      depth <- Gen.choose(0, 3)
      expr  <- Gen.frequency(
        (100, helper(depth)),
        (1,   genFilterAST)
      )
    } yield Filter(Refined.unsafeApply(name), Refined.unsafeApply(ppFilter(expr)))

  }

}

object FilterGenerators extends FilterGenerators