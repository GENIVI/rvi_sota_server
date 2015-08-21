/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.db

import slick.driver.MySQLDriver.api._

object Operators {
  val regex = SimpleBinaryOperator[Boolean]("REGEXP")
}
