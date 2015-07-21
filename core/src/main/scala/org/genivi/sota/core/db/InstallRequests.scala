package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.InstallRequest

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
