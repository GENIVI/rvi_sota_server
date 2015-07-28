package org.genivi.sota.resolver.test

import akka.actor.ActorSystem
import org.flywaydb.core.Flyway
import slick.driver.MySQLDriver.api._

object TestDatabase {
  val name = "test-database"

  import slick.driver.MySQLDriver.api._
  val db = Database.forConfig(name)
  implicit val session: Session = db.createSession()

  def reset(implicit system: ActorSystem) = {
    val dbConfig = system.settings.config.getConfig(name)
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
