/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.db

import org.genivi.sota.http.Errors

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._

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

  /**
    * It's occasionally useful to turn a database operation that might
    * not return something into an exception.
    *
    * @param io The database operation that might fail to deliver a value.
    * @param t The exception to throw when the database operation fails.
    * @return The value when it succeeds, otherwise throw the exception.
    */
  implicit class DBIOOps[T](io: DBIO[Option[T]]) {

    def failIfNone(t: Throwable)
                  (implicit ec: ExecutionContext): DBIO[T] =
      io.flatMap(_.fold[DBIO[T]](DBIO.failed(t))(DBIO.successful))
  }

  implicit class DBIOSeqOps[+T](io: DBIO[Seq[T]]) {
    def failIfNotSingle(t: Throwable)
                       (implicit ec: ExecutionContext): DBIO[T] = {
      val dbio = io.flatMap { result =>
        if(result.size > 1)
          DBIO.failed(Errors.TooManyElements)
        else
          DBIO.successful(result.headOption)
      }

      DBIOOps(dbio).failIfNone(t)
    }
  }

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
