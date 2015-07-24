package org.genivi.sota.resolver.db

trait DatabaseConfig {
  val driver = slick.driver.MySQLDriver

  import driver.api.{Database, Session}

  val db = Database.forConfig("database")

  implicit val session: Session = db.createSession()
}
