/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{Regex, regexValidate}
import org.genivi.sota.data.{PackageId, Vehicle}
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import scala.util.parsing.combinator.{PackratParsers, ImplicitConversions}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.packages.Package


sealed trait FilterAST
final case class VinMatches(vin: Refined[String, Regex])    extends FilterAST
final case class HasPackage(
  name   : Refined[String, Regex],
  version: Refined[String, Regex])                          extends FilterAST
final case class HasComponent(name: Refined[String, Regex]) extends FilterAST
final case class Not(f: FilterAST)                          extends FilterAST
final case class And(l: FilterAST, r: FilterAST)            extends FilterAST
final case class Or(l: FilterAST,  r: FilterAST)            extends FilterAST
final case object True                                      extends FilterAST
final case object False                                     extends FilterAST


object FilterAST extends StandardTokenParsers with PackratParsers with ImplicitConversions {

  lexical.delimiters ++= List("(", ")")
  lexical.reserved   ++= List("vin_matches", "has_package", "has_component", "NOT", "AND", "OR", "TRUE", "FALSE")

  lazy val vinP: PackratParser[FilterAST] =
    "vin_matches" ~> regexLit ^^ VinMatches

  lazy val pkgP: PackratParser[FilterAST] =
    "has_package" ~> regexLit ~ regexLit ^^ HasPackage

  lazy val compP: PackratParser[FilterAST] =
    "has_component" ~> regexLit ^^ HasComponent

  lazy val trueP: PackratParser[FilterAST] =
    "TRUE" ^^^ True

  lazy val falseP: PackratParser[FilterAST] =
    "FALSE" ^^^ False

  lazy val leafP: PackratParser[FilterAST] = vinP | pkgP | compP | trueP | falseP

  lazy val primP: PackratParser[FilterAST] =
    "(" ~> orP <~ ")" | leafP

  lazy val unaryP : PackratParser[FilterAST] =
    "NOT" ~> primP ^^ Not | primP

  lazy val andP: PackratParser[FilterAST] =
    (unaryP ~ ("AND" ~> andP)) ^^ And | unaryP

  lazy val orP: PackratParser[FilterAST] =
    (andP ~ ("OR" ~> orP)) ^^ Or | andP

  def regexLit: Parser[Refined[String, Regex]] =
    elem("regex", t => regexValidate.isValid(t.chars)) ^^ (t => Refined.unsafeApply(t.chars))

  def parseFilter(input: String): Either[String, FilterAST] =
    phrase(orP)(new lexical.Scanner(input)) match {
      case Success(f, _)        => Right(f)
      case NoSuccess(msg, next) => Left(s"\n${next.pos.longString}: ${msg}")
    }

  def parseValidFilter(input: Filter.Expression): FilterAST =
    parseFilter(input.get) match {
      case Right(f) => f

      // The very definition of being a valid filter expression is that it parses.
      case Left(_)  => sys.error("parseValidFilter: IMPOSSIBLE")
    }

  def ppFilter(f: FilterAST): String =
    f match {
      case VinMatches(s)    => s"""vin_matches "$s""""
      case HasPackage(s, t) => s"""has_package "$s" "$t""""
      case HasComponent(s)  => s"""has_component "$s""""
      case Not(f)           => s"NOT (${ppFilter(f)})"
      case And(l, r)        => s"(${ppFilter(l)}) AND (${ppFilter(r)})"
      case Or (l, r)        => s"(${ppFilter(l)}) OR (${ppFilter(r)})"
      case True             => "TRUE"
      case False            => "FALSE"
    }

  def query(f: FilterAST): Function1[(Vehicle, (Seq[PackageId], Seq[Component.PartNumber])), Boolean] =
  { case a@((v: Vehicle, (ps: Seq[PackageId], cs: Seq[Component.PartNumber]))) => f match {
      case VinMatches(re)       => !re.get.r.findAllIn(v.vin.get).isEmpty
      case HasPackage(re1, re2) => ps.map(p => !re1.get.r.findAllIn(p.name   .get).isEmpty &&
                                               !re2.get.r.findAllIn(p.version.get).isEmpty)
                                     .exists(_== true)
      case HasComponent(re)     => cs.map(part => !re.get.r.findAllIn(part.get).isEmpty)
                                     .exists(_== true)
      case Not(f)               => !query(f)(a)
      case And(l, r)            => query(l)(a) && query(r)(a)
      case Or (l, r)            => query(l)(a) || query(r)(a)
      case True                 => true
      case False                => false
    }
  }

}
