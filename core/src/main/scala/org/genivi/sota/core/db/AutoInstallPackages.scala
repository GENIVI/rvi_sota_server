/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.AutoInstall
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api._

object AutoInstalls {
  import org.genivi.sota.refined.SlickRefined._
  import org.genivi.sota.db.SlickExtensions._

  class AutoInstallTable(tag: Tag) extends Table[AutoInstall](tag, "AutoInstall") {
    def namespace = column[Namespace]("namespace")
    def pkgName = column[PackageId.Name]("pkg_name")
    def device = column[Uuid]("device")

    def pk = primaryKey("pk_auto_install", (namespace, pkgName, device))

    def * = (namespace, pkgName, device).shaped <>
      ((AutoInstall.apply _).tupled, AutoInstall.unapply)
  }

  val all = TableQuery[AutoInstallTable]

  def listDevices(ns: Namespace, pkgName: PackageId.Name): DBIO[Seq[Uuid]]
    = all.filter(_.namespace === ns)
      .filter(_.pkgName === pkgName)
      .map(_.device)
      .result

  def listPackages(ns: Namespace, device: Uuid): DBIO[Seq[PackageId.Name]]
    = all.filter(_.namespace === ns)
    .filter(_.device === device)
    .map(_.pkgName)
    .result

  def removeAll(ns: Namespace, pkgName: PackageId.Name)
               (implicit ec: ExecutionContext): DBIO[Unit]
    = all.filter(_.namespace === ns)
      .filter(_.pkgName === pkgName)
      .delete
      .map(_ => ())

  def addDevice(ns: Namespace, pkgName: PackageId.Name, dev: Uuid)
               (implicit ec: ExecutionContext): DBIO[Unit]
    //This code is a workaround for a bug in Slick's insertOrUpdate() See Slick Issue #1728
    = all
    .filter(_.namespace === ns)
    .filter(_.pkgName === pkgName)
    .filter(_.device === dev)
    .result
    .flatMap { result =>
      if(result.isEmpty) {
        (all += AutoInstall(ns, pkgName, dev)).map(_ => ())
      } else {
        DBIO.successful(())
      }
    }.transactionally

  def removeDevice(ns: Namespace, pkgName: PackageId.Name, dev: Uuid)
                  (implicit ec: ExecutionContext): DBIO[Unit]
    = all.filter(_.namespace === ns)
      .filter(_.pkgName === pkgName)
      .filter(_.device === dev)
      .delete
      .map(_ => ())
}
