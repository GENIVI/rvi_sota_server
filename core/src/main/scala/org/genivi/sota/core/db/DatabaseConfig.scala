package org.genivi.sota.core.db

import java.sql.Timestamp

import org.joda.time.DateTime

trait DatabaseConfig {
  val driver = slick.driver.MySQLDriver

  import driver.api.{Database, Session}

  implicit val jodaDateTimeMapping = {
    import driver.api._
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }

  def db = Database.forConfig("database")

  implicit val session: Session = db.createSession()
}
