/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.Vin
import scala.concurrent.ExecutionContext

object Vins {

  import slick.driver.MySQLDriver.api._

  // scalastyle:off
  class VinTable(tag: Tag) extends Table[Vin](tag, "Vin") {
    def vin = column[String]("vin")
    def * = (vin) <> (Vin.apply, Vin.unapply)
    def pk = primaryKey("vin", vin)  // insertOrUpdate doesn't work if
                                     // we use O.PrimaryKey in the vin
                                     // column, see Slick issue #966.
  }
  // scalastyle:on

  val vins = TableQuery[VinTable]

  def add(vin: Vin.ValidVin)(implicit ec: ExecutionContext): DBIO[Vin] =
    vins.insertOrUpdate(vin).map(_ => vin)

  def list: DBIO[Seq[Vin]] =
    vins.result
}
