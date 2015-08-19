/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.types.Vehicle
import scala.concurrent.ExecutionContext

object Vehicles {

  import slick.driver.MySQLDriver.api._

  // scalastyle:off
  class VinTable(tag: Tag) extends Table[Vehicle](tag, "Vehicle") {
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def * = (vin) <> (Vehicle.apply, Vehicle.unapply)
    def pk = primaryKey("vin", vin)  // insertOrUpdate doesn't work if
                                     // we use O.PrimaryKey in the vin
                                     // column, see Slick issue #966.
  }
  // scalastyle:on

  val vehicles = TableQuery[VinTable]

  def add(vehicle: Vehicle)
         (implicit ec: ExecutionContext): DBIO[Vehicle] =
    vehicles.insertOrUpdate(vehicle).map(_ => vehicle)

  def list: DBIO[Seq[Vehicle]] =
    vehicles.result
}
