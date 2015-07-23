package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.InstallRequest
import org.genivi.sota.core.Package

object InstallRequests extends DatabaseConfig {

  class InstallRequestTable(tag: Tag) extends Table[InstallRequest](tag, "InstallRequest") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def installCampaignId = column[Long]("installCampaignId")
    def packageId = column[Long]("packageId")
    def vin = column[String]("vin")
    def statusCode = column[Char]("statusCode")
    def errorMessage = column[String]("errorMessage")

    def * = (id.?, installCampaignId, packageId, vin, statusCode, errorMessage.?) <>
      ((InstallRequest.apply _).tupled, InstallRequest.unapply)
  }

  val installRequests = TableQuery[InstallRequestTable]

  def list: Future[Seq[InstallRequest]] = db.run(installRequests.result)

  def currentAt(instant: DateTime): Future[Seq[(InstallRequest, Package)]] = {
    val installCampaigns = InstallCampaigns.installCampaigns
    val packages = Packages.packages
    val q = for {
      c <- installCampaigns if c.startAfter < instant && c.endBefore > instant
      r <- installRequests if r.statusCode === '0' && r.installCampaignId === c.id
      p <- packages if r.packageId === p.id
    } yield (r,p)
    db.run(q.result)
  }

  def updateNotified(reqs: Seq[InstallRequest])(implicit ec: ExecutionContext): Future[Int] = {
    val reqIds = reqs.map(_.id.get)
    val updateInstallRequests = for {
      r <- installRequests if r.id inSetBind(reqIds)
    } yield r.statusCode
    db.run(updateInstallRequests.update('1'))
  }

  def create(installRequest: InstallRequest)(implicit ec: ExecutionContext): Future[InstallRequest] =
    create(List(installRequest)).map(_.head)

  def create(reqs: Seq[InstallRequest]): Future[Seq[InstallRequest]] = {
    val insertions = reqs.map { installRequest =>
      (installRequests
         returning installRequests.map(_.id)
         into ((request, id) => request.copy(id = Some(id)))) += installRequest
    }

    db.run(DBIO.sequence(insertions))
  }
}
