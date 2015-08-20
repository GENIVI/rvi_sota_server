/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.{Package, InstallRequest, Vehicle, PackageId}
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.dbio.DBIO
import slick.driver.JdbcTypesComponent._
import slick.driver.MySQLDriver.api._

object InstallRequests {

  import Mappings._
  import InstallRequest.Status

  implicit val statusCodeMapper = MappedColumnType.base[Status, Int](_.value.id, s => Status(s))
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  class InstallRequestTable(tag: Tag) extends Table[InstallRequest](tag, "InstallRequest") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def installCampaignId = column[Long]("installCampaignId")
    def packageName = column[Package.Name]("packageName")
    def packageVersion = column[Package.Version]("packageVersion")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def statusCode = column[Status]("statusCode")
    def errorMessage = column[String]("errorMessage")
    def * = (id.?, installCampaignId, packageName, packageVersion, vin, statusCode, errorMessage.?).shaped <>
      (row => InstallRequest(row._1, row._2, PackageId(row._3, row._4), row._5, row._6, row._7), (x: InstallRequest) => Some((x.id, x.installCampaignId, x.packageId.name, x.packageId.version, x.vin, x.statusCode, x.errorMessage)))

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
      p <- packages if r.packageName === p.name && r.packageVersion === p.version
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
