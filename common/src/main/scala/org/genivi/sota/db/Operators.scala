/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.db

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object Operators {

  val regex = SimpleBinaryOperator[Boolean]("REGEXP")

  implicit class DBIOOps[T](io: DBIO[Option[T]]) {

    def failIfNone(t: Throwable)
                  (implicit ec: ExecutionContext): DBIO[T] =
      io.flatMap(_.fold[DBIO[T]](DBIO.failed(t))(DBIO.successful))
  }

}
