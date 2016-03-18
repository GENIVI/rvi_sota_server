/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.scalatest.{Suite, BeforeAndAfterAll}
import slick.driver.MySQLDriver.api._

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

trait DatabaseSpec extends BeforeAndAfterAll {
  self: Suite ⇒

  private val databaseId = "test-database"

  lazy val db = Database.forConfig(databaseId)

  override def beforeAll() {
    TestDatabase.resetDatabase(databaseId)
    super.beforeAll()
  }

  override def afterAll() {
    db.close()
    super.afterAll()
  }
}
