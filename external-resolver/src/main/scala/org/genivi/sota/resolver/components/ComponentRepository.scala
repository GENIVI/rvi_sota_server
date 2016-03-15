/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.db.Operators._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.vehicles.VehicleRepository
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api._


object ComponentRepository {

  // scalastyle:off
  private[components] class ComponentTable(tag: Tag)
      extends Table[Component](tag, "Component") {

    def partNumber  = column[Component.PartNumber]("partNumber")
    def description = column[String]              ("description")

    def pk = primaryKey("pk_component", partNumber)

    def * = (partNumber, description) <>
      ((Component.apply _).tupled, Component.unapply)
  }
  // scalastyle:on

  val components = TableQuery[ComponentTable]

  def addComponent(comp: Component): DBIO[Int] =
    components.insertOrUpdate(comp)

  def removeComponent(part: Component.PartNumber)
                     (implicit ec: ExecutionContext): DBIO[Int] =
    for {
      vs <- VehicleRepository.search(None, None, None, Some(part))
      r  <- if (vs.nonEmpty) DBIO.failed(Errors.ComponentIsInstalledException)
            else components.filter(_.partNumber === part).delete
    } yield r

  def exists(part: Component.PartNumber)
            (implicit ec: ExecutionContext): DBIO[Component] =
    components
      .filter(_.partNumber === part)
      .result
      .headOption
      .failIfNone(Errors.MissingComponent)

  def list: DBIO[Seq[Component]] =
    components.result

  def searchByRegex(re: Refined[String, Regex]): DBIO[Seq[Component]] =
    components.filter(comp => regex(comp.partNumber, re.get)).result

}
