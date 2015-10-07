/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.Vehicle
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.Operators.regex

object Vehicles {
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  class VehicleTable(tag: Tag) extends Table[Vehicle](tag, "Vehicle") {
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def * = (vin) <> (Vehicle.apply, Vehicle.unapply)
    def pk = primaryKey("vin", vin)  // insertOrUpdate doesn't work if
                                     // we use O.PrimaryKey in the vin
                                     // column, see Slick issue #966.
  }
  // scalastyle:on

  val vins = TableQuery[VehicleTable]

  def list(): DBIO[Seq[Vehicle]] = vins.result

  def exists(vin: Vehicle.IdentificationNumber): DBIO[Option[Vehicle]] =
    vins
      .filter(_.vin === vin)
      .result
      .headOption

  def create(vehicle: Vehicle)(implicit ec: ExecutionContext) : DBIO[Vehicle.IdentificationNumber] =
    vins.insertOrUpdate(vehicle).map( _ => vehicle.vin )

  def deleteById(vehicle : Vehicle) : DBIO[Int] = vins.filter( _.vin === vehicle.vin ).delete

  def searchByRegex(reg:String) : DBIO[Seq[Vehicle]] = vins.filter(vins => regex(vins.vin, reg)).result
}
