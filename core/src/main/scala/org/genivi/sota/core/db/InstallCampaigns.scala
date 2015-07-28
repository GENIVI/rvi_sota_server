/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.InstallCampaign

object InstallCampaigns {

  import Mappings._

  // scalastyle:off
  class InstallCampaignTable(tag: Tag) extends Table[InstallCampaign](tag, "InstallCampaign") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def packageId = column[Long]("packageId")
    def priority = column[Int]("priority")
    def startAfter = column[DateTime]("startAfter")
    def endBefore = column[DateTime]("endBefore")

    def * = (id.?, packageId, priority, startAfter, endBefore) <>
      ((InstallCampaign.apply _).tupled, InstallCampaign.unapply)
  }
  // scalastyle:on

  val installCampaigns = TableQuery[InstallCampaignTable]

  def list(): DBIO[Seq[InstallCampaign]] = installCampaigns.result

  def create(installCampaign: InstallCampaign): DBIO[InstallCampaign] =
    (installCampaigns returning installCampaigns.map(_.id)
      into ((request, id) => request.copy(id = Some(id)))) += installCampaign
}
