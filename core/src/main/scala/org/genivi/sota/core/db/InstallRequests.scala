/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.dbio.DBIO
import slick.driver.JdbcTypesComponent._
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.InstallRequest
import org.genivi.sota.core.Package

object InstallRequests {

  import Mappings._
  import InstallRequest.Status

  implicit val statusCodeMapper = MappedColumnType.base[Status, Int](_.value.id, s => Status(s))

  // scalastyle:off
  class InstallRequestTable(tag: Tag) extends Table[InstallRequest](tag, "InstallRequest") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def installCampaignId = column[Long]("installCampaignId")
    def packageId = column[Long]("packageId")
    def vin = column[String]("vin")
    def statusCode = column[Status]("statusCode")
    def errorMessage = column[String]("errorMessage")

    def * = (id.?, installCampaignId, packageId, vin, statusCode, errorMessage.?) <>
      ((InstallRequest.apply _).tupled, InstallRequest.unapply)
  }
  // scalastyle:on

  val installRequests = TableQuery[InstallRequestTable]

  def list(): DBIO[Seq[InstallRequest]] = installRequests.result
  def list(ids: Seq[Long]): DBIO[Seq[InstallRequest]] = installRequests.filter(_.id inSetBind(ids)).result

  def currentAt(instant: DateTime): DBIO[Seq[(InstallRequest, Package)]] = {
    val installCampaigns = InstallCampaigns.installCampaigns
    val packages = Packages.packages
    val q = for {
      c <- installCampaigns if c.startAfter < instant && c.endBefore > instant
      r <- installRequests if r.statusCode === Status.NotProcessed && r.installCampaignId === c.id
      p <- packages if r.packageId === p.id
    } yield (r,p)
    q.result
  }

  def updateNotified(reqs: Seq[InstallRequest]): DBIO[Int] = {
    val reqIds = reqs.map(_.id.get).toSet
    installRequests.
      filter(_.id inSetBind(reqIds)).
      map(_.statusCode).
      update(Status.Notified)
  }

  def create(installRequest: InstallRequest): DBIO[InstallRequest] =
    (installRequests returning installRequests.map(_.id)
      into ((request, id) => request.copy(id = Some(id)))) += installRequest

  def createRequests(reqs: Seq[InstallRequest]): DBIO[Seq[InstallRequest]] = DBIO.sequence( reqs.map( create ) )

}
