/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.Namespace
import org.genivi.sota.data.Namespace._
import org.genivi.sota.db.Operators._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.db.DeviceRepository

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._


object ComponentRepository {

  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  private[components] class ComponentTable(tag: Tag)
    extends Table[Component](tag, "Component") {

    def namespace = column[Namespace]("namespace")

    def partNumber = column[Component.PartNumber]("partNumber")

    def description = column[String]("description")

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
                     (implicit db: Database, ec: ExecutionContext): Future[Int] =
      isInstalled(namespace, part) flatMap { is =>
      if (is) {
        Future.failed(Errors.ComponentInstalled)
      } else {
        val dbIO = components
          .filter(_.namespace === namespace)
          .filter(_.partNumber === part)
          .delete

        db.run(dbIO)
      }
    }

  def isInstalled(namespace: Namespace, part: Component.PartNumber)
                 (implicit db: Database, ec: ExecutionContext): Future[Boolean] = {
     val dbIO = DeviceRepository.installedComponents
       .filter(_.namespace === namespace)
       .filter(_.partNumber === part)
       .exists
       .result

    db.run(dbIO)
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
