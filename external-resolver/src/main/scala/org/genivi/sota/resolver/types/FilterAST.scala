/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import org.genivi.sota.resolver.types.Filter.Expression
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import scala.util.parsing.combinator.{PackratParsers, ImplicitConversions}


sealed abstract trait FilterAST
case class VinMatches(vin: String)                   extends FilterAST
case class HasPackage(name: String, version: String) extends FilterAST
case class HasComponent(name: String)                extends FilterAST
case class Not(f: FilterAST)                         extends FilterAST
case class And(l: FilterAST, r: FilterAST)           extends FilterAST
case class Or(l: FilterAST,  r: FilterAST)           extends FilterAST
case object True                                     extends FilterAST
case object False                                    extends FilterAST


object FilterParser extends StandardTokenParsers with PackratParsers with ImplicitConversions {

  lexical.delimiters ++= List("(",")")
  lexical.reserved   ++= List("vin_matches", "has_package", "has_component", "NOT", "AND", "OR", "TRUE", "FALSE")

  lazy val vinP: PackratParser[FilterAST] =
    "vin_matches" ~> stringLit ^^ VinMatches

  lazy val pkgP: PackratParser[FilterAST] =
    "has_package" ~> stringLit ~ stringLit ^^ HasPackage

  lazy val compP: PackratParser[FilterAST] =
    "has_component" ~> stringLit ^^ HasComponent

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


  def parseFilter(input: String): Either[String, FilterAST] =
    phrase(orP)(new lexical.Scanner(input)) match {
      case Success(f, _)        => Right(f)
      case NoSuccess(msg, next) => Left(s"Could not parse '${input}' near '${next.pos.longString} : ${msg}")
    }

  def parseValidFilter(input: Filter.Expression): FilterAST =
    parseFilter(input.get) match {
      case Right(f) => f

      // The very definition of being a valid filter expression is that it parses.
      case Left(_)  => sys.error("parseValidFilter: IMPOSSIBLE")
    }
}

object FilterPrinter {

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
}

object FilterQuery {

  def query(f: FilterAST): Function1[Vehicle, Boolean] = (v: Vehicle) => f match {
    case VinMatches(s)    => !s.r.findAllIn(v.vin.get).isEmpty
    case HasPackage(s, t) => true           // XXX: FOR NOW
    case HasComponent(s)  => true           // XXX: FOR NOW
    case Not(f)           => !query(f)(v)
    case And(l, r)        => query(l)(v) && query(r)(v)
    case Or (l, r)        => query(l)(v) || query(r)(v)
    case True             => true
    case False            => false
  }
}
