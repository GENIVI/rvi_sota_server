/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.db

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

}
