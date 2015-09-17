/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.{InstallCampaign, Package}
import org.joda.time.DateTime
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._

object InstallCampaigns {

  import Mappings._
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  class InstallCampaignTable(tag: Tag) extends Table[InstallCampaign](tag, "InstallCampaign") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def packageName = column[Package.Name]("packageName")
    def packageVersion = column[Package.Version]("packageVersion")
    def priority = column[Int]("priority")
    def startAfter = column[DateTime]("startAfter")
    def endBefore = column[DateTime]("endBefore")

    def * = (id.?, packageName, packageVersion, priority, startAfter, endBefore).shaped <>
      (row => InstallCampaign(row._1, Package.Id(row._2, row._3), row._4, row._5, row._6),
      (x: InstallCampaign) => Some((x.id, x.packageId.name, x.packageId.version, x.priority, x.startAfter, x.endBefore)) )
  }
  // scalastyle:on

  val installCampaigns = TableQuery[InstallCampaignTable]

  def list(): DBIO[Seq[InstallCampaign]] = installCampaigns.result

  def create(installCampaign: InstallCampaign): DBIO[InstallCampaign] =
    (installCampaigns returning installCampaigns.map(_.id)
      into ((request, id) => request.copy(id = Some(id)))) += installCampaign
}
