package org.genivi.sota.core.db

import java.sql.Timestamp

import org.joda.time.DateTime
import slick.driver.MySQLDriver.api._

object Mappings {
  implicit val jodaDateTimeMapping = {
    
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts))
  }
}

trait DatabaseConfig {

  def db = Database.forConfig("database")

  implicit val session: Session = db.createSession()
}
