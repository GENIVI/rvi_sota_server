/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.vehicles.{Vehicle, VehicleRepository}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.Operators._


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

  def exists(part: Component.PartNumber)
            (implicit ec: ExecutionContext): DBIO[Component] =
    components
      .filter(_.partNumber === part)
      .result
      .headOption
      .failIfNone(Errors.MissingComponent)

}
