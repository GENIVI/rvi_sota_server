/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import scala.util.parsing.combinator.{PackratParsers, ImplicitConversions}
import scala.util.parsing.combinator.syntactical.StandardTokenParsers


sealed abstract trait FilterAST
case class VinMatches(vin: String)                   extends FilterAST
case class HasPackage(name: String, version: String) extends FilterAST
case class HasComponent(name: String)                extends FilterAST
case class Not(f: FilterAST)                         extends FilterAST
case class And(l: FilterAST, r: FilterAST)           extends FilterAST
case class Or(l: FilterAST,  r: FilterAST)           extends FilterAST


object FilterParser extends StandardTokenParsers with PackratParsers with ImplicitConversions {

  lexical.delimiters ++= List("(",")")
  lexical.reserved   ++= List("vin_matches", "has_package", "has_component", "NOT", "AND", "OR")

  lazy val vinP: PackratParser[FilterAST] =
    "vin_matches" ~> stringLit ^^ VinMatches

  lazy val pkgP: PackratParser[FilterAST] =
    "has_package" ~> stringLit ~ stringLit ^^ HasPackage

  lazy val compP: PackratParser[FilterAST] =
    "has_component" ~> stringLit ^^ HasComponent

  lazy val leafP: PackratParser[FilterAST] = vinP | pkgP | compP

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
    }
}
