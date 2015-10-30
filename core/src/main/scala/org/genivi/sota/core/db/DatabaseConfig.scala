/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.sql.Timestamp

import org.joda.time.DateTime
import slick.driver.MySQLDriver.api._

/**
 * Define how to store a Joda Date time in the SQL database.
 * This is imported as an implicit into the other database mapping definitions
 * in this directory.
 */
object Mappings {
  implicit val jodaDateTimeMapping = {
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }
}

/**
 * Define how to open a database connection.  This information is read out of
 * the default configuration.
 * It is a scala trait that will be mixed into the main app (in Boot.scala)
 */
trait DatabaseConfig {
  val db = Database.forConfig("database")

  implicit val session: Session = db.createSession()
}
