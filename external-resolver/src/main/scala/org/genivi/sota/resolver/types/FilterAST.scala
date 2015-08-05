/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.syntactical.StandardTokenParsers


sealed abstract trait FilterAST
case class VinMatches(vin: String)         extends FilterAST
case class Or(l: FilterAST,  r: FilterAST) extends FilterAST
case class And(l: FilterAST, r: FilterAST) extends FilterAST


object FilterParser extends StandardTokenParsers with PackratParsers {

  lexical.delimiters ++= List("(",")")
  lexical.reserved   ++= List("vin_matches", "AND", "OR")

  val filterP: PackratParser[FilterAST] = orP | andP | leafP | "("~>filterP<~")"
  val orP    : PackratParser[FilterAST] = filterP~("OR"~>filterP)            ^^ { case l~r => Or(l, r)      }
  val andP   : PackratParser[FilterAST] = filterP~("AND"~>(leafP | filterP)) ^^ { case l~r => And(l, r)     }
  val vinP   : PackratParser[FilterAST] = "vin_matches"~stringLit            ^^ { case _~s => VinMatches(s) }
  val leafP  : PackratParser[FilterAST] = vinP

  def parseFilter(input: String): Either[String, FilterAST] =
    phrase(filterP)(new lexical.Scanner(input)) match {
      case Success(f, _)        => Right(f)
      case NoSuccess(msg, next) => Left(s"Could not parse '${input}' near '${next.pos.longString} : ${msg}")
    }
}

object FilterPrinter {

  def ppFilter(f: FilterAST): String =
    f match {
      case VinMatches(s) => s"""vin_matches "$s""""
      case Or (l, r)     => s"(${ppFilter(l)}) OR (${ppFilter(r)})"
      case And(l, r)     => s"(${ppFilter(l)}) AND (${ppFilter(r)})"
    }
}
