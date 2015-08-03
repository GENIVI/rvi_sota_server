/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.Vin
import scala.concurrent.ExecutionContext

object Vins {

  import slick.driver.MySQLDriver.api._

  class VinTable(tag: Tag) extends Table[Vin](tag, "Vin") {
    def vin = column[String]("vin", O.PrimaryKey)
    def * = (vin) <> (Vin.apply, Vin.unapply)
  }

  val vins = TableQuery[VinTable]

  def add(vin: Vin.ValidVin)(implicit ec: ExecutionContext): DBIO[Vin] = {
    (vins += vin).map(_ => vin)
  }

  def list: DBIO[Seq[Vin]] =
    vins.result

  def create: DBIO[Unit] =
    vins.schema.create
}
