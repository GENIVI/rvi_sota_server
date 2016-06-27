/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.Namespace._
import org.genivi.sota.db.Operators._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.vehicles.VehicleRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api._


object ComponentRepository {

  // scalastyle:off
  private[components] class ComponentTable(tag: Tag)
      extends Table[Component](tag, "Component") {

    def namespace   = column[Namespace]           ("namespace")
    def partNumber  = column[Component.PartNumber]("partNumber")
    def description = column[String]              ("description")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_component", (namespace, partNumber))

    def * = (namespace, partNumber, description) <>
      ((Component.apply _).tupled, Component.unapply)
  }
  // scalastyle:on

  val components = TableQuery[ComponentTable]

  def addComponent(comp: Component): DBIO[Int] =
    components.insertOrUpdate(comp)

  def removeComponent(namespace: Namespace, part: Component.PartNumber)
                     (implicit db: Database, ec: ExecutionContext, mat: ActorMaterializer): Future[Int] =
    // TODO: namespace
    VehicleRepository.search(db, namespace, None, None, None, Some(part)) flatMap { vs =>
      if (vs.nonEmpty) {
        Future.failed(Errors.ComponentIsInstalledException)
      } else {
        val dbIO = components.filter (i => i.namespace === namespace && i.partNumber === part).delete
        db.run(dbIO)
      }
    }

  def exists(namespace: Namespace,
             part: Component.PartNumber)
            (implicit ec: ExecutionContext): DBIO[Component] =
    components
      .filter(i => i.namespace === namespace && i.partNumber === part)
      .result
      .headOption
      .failIfNone(Errors.MissingComponent)

  def list: DBIO[Seq[Component]] =
    components.result

  def searchByRegex(namespace: Namespace, re: Refined[String, Regex]): DBIO[Seq[Component]] =
    components.filter(i => i.namespace === namespace && regex(i.partNumber, re.get)).result

}
