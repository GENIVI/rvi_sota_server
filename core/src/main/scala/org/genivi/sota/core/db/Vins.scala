package org.genivi.sota.core.db

import org.genivi.sota.core.Vin
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._

object Vins {

  class VinTable(tag: Tag) extends Table[Vin](tag, "Vin") {
    def vin = column[String]("vin", O.PrimaryKey)
    def * = (vin) <> (Vin.apply, Vin.unapply)
  }

  val vins = TableQuery[VinTable]

  def list() = vins.result

  def create(vin: Vin) : DBIOAction[Int, NoStream, Effect.Write] = vins += vin
}
