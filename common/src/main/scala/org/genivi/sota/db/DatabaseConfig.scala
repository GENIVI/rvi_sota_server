/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */
package org.genivi.sota.db

import slick.jdbc.MySQLProfile.api._

/**
 * Define how to open a database connection.  This information is read out of
 * the default configuration.
 * It is a scala trait that will be mixed into the main app (in Boot.scala)
 */
trait DatabaseConfig {
  val db = Database.forConfig("database")
}
