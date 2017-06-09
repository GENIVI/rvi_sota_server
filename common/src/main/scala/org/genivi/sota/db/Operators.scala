/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.db

import slick.jdbc.MySQLProfile.api._

/**
  * Some database operators are shared between the core and the
  * resolver.
  */

object Operators {

  /**
    * The database layer, Slick, doesn't know that MariaDB supports
    * regex search.
    *
    * @see {@link http://slick.typesafe.com/docs/}
    * @see {@link https://mariadb.com/kb/en/mariadb/regexp/}
    */
  val regex = SimpleBinaryOperator[Boolean]("REGEXP")

  implicit class QueryReg[QTable, Row, S[_]](baseQuery: Query[QTable, Row, S]) {
    def regexFilter(reg: Option[String])(fieldsFn: (QTable => Rep[_])*): Query[QTable, Row, S] = {
      reg match {
        case Some(r) =>
          baseQuery.filter { table =>
            fieldsFn.foldLeft(false.bind) { case (acc, rep) => acc || regex(rep(table), r) }
          }
        case None => baseQuery
      }
    }
  }
}
