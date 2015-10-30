/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway

/*
 * Helper object to configure test database for specs
 */
object TestDatabase {

  def resetDatabase( databaseName: String ) = {
    val dbConfig = ConfigFactory.load().getConfig(databaseName)
    val url = dbConfig.getString("url")
    val user = dbConfig.getConfig("properties").getString("user")
    val password = dbConfig.getConfig("properties").getString("password")

    val flyway = new Flyway
    flyway.setDataSource(url, user, password)
    flyway.setLocations("classpath:db.migration")
    flyway.clean()
    flyway.migrate()
  }
}
