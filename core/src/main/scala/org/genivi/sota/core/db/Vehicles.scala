/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.Vehicle
import org.genivi.sota.db.Operators.regex
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import slick.lifted.TableQuery


/**
 * Database mapping definition for the Vehicles table.
 * The Vehicles table is simple: it only contains the list of the vehicles
 * (VINs) that are known to the SOTA system.  Other tables that refer to
 * VINs should have a foreign key into this table
 */
object Vehicles {
  import org.genivi.sota.refined.SlickRefined._
  import org.genivi.sota.db.SlickExtensions._

  /**
   * Slick mapping definition for the Vehicle table
   * @see {@link http://slick.typesafe.com/}
   */
  // scalastyle:off
  class VehicleTable(tag: Tag) extends Table[Vehicle](tag, "Vehicle") {
    def namespace = column[Namespace]("namespace")
    def vin = column[Vehicle.Vin]("vin")
    def lastSeen = column[Option[DateTime]]("last_seen")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("vin", (namespace, vin))

    def * = (namespace, vin, lastSeen) <> (Vehicle.tupled, Vehicle.unapply)
  }
  // scalastyle:on

  /**
   * Internal helper definition to access the SQL table
   */
  val vehicles = TableQuery[VehicleTable]

  /**
   * List all the VINs that are known to the system
   * @return A list of Vehicles
   */
  def list(): DBIO[Seq[Vehicle]] = vehicles.result


  def all(namespace: Namespace): Query[VehicleTable, Vehicle, Seq] = vehicles.filter(_.namespace === namespace)

  /**
   * Check if a VIN exists
   * @param vehicle The namespaced VIN to search for
   * @return Option.Some if the vehicle is present. Option.None if it is absent
   */
  def exists(vehicle: Vehicle): DBIO[Option[Vehicle]] =
    vehicles
      .filter(v => v.namespace === vehicle.namespace && v.vin === vehicle.vin)
      .result
      .headOption

  /**
   * Add a new VIN to SOTA.
   * @param vehicle The VIN of the vehicle
   */
  def create(vehicle: Vehicle)(implicit ec: ExecutionContext) : DBIO[Vehicle] =
    vehicles.insertOrUpdate(vehicle).map(_ => vehicle)

  /**
   * Delete a VIN from SOTA.
   * Note that this doesn't perform a cascading delete.  You should delete any
   * objects that reference this vehicle first.
   * @param vehicle The VIN to remove
   */
  def deleteById(vehicle : Vehicle) : DBIO[Int] =
    vehicles.filter(v => v.namespace === vehicle.namespace && v.vin === vehicle.vin).delete

  /**
   * Find VINs that match a regular expression
   * @param reg A regular expression
   * @return A list of matching VINs
   */
  def searchByRegex(ns: Namespace, reg:String): Query[VehicleTable, Vehicle, Seq] =
    all(ns).filter(v => regex(v.vin, reg))

  def updateLastSeen(vin: Vehicle.Vin, lastSeen: DateTime = DateTime.now)
                    (implicit ec: ExecutionContext): DBIO[DateTime] = {
    vehicles
      .filter(_.vin === vin)
      .map(_.lastSeen)
      .update(Some(lastSeen))
      .map(_ => lastSeen)
  }

  def findBy(vehicle: Vehicle): DBIO[Vehicle] = {
    vehicles
      .filter(v => v.namespace === vehicle.namespace && v.vin === vehicle.vin)
      .result
      .head
  }
}
