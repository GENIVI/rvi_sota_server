/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.Vin
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._

object Vins {

  // scalastyle:off
  class VinTable(tag: Tag) extends Table[Vin](tag, "Vin") {
    def vin = column[String]("vin", O.PrimaryKey)
    def * = (vin) <> (Vin.apply, Vin.unapply)
  }
  // scalastyle:on

  val vins = TableQuery[VinTable]

  def list(): DBIO[Seq[Vin]] = vins.result

  def create(vin: Vin) : DBIO[Int] = vins += vin
}
