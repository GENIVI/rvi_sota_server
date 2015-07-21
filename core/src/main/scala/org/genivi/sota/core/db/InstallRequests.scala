package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.InstallRequest

object InstallRequests extends DatabaseConfig {

  class InstallRequestsTable(tag: Tag) extends Table[InstallRequest](tag, "install_requests") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def packageId = column[Long]("package_id")
    def priority = column[Int]("priority")
    def startAfter = column[DateTime]("start_after")
    def endBefore = column[DateTime]("end_before")

    def * = (id.?, packageId, priority, startAfter, endBefore) <>
      ((InstallRequest.apply _).tupled, InstallRequest.unapply)
  }

  val installRequests = TableQuery[InstallRequestsTable]

  def init: Future[Unit] = db.run(installRequests.schema.create)

  def list: Future[Seq[InstallRequest]] = db.run(installRequests.result)

  def create(installRequest: InstallRequest): Future[InstallRequest] = {
    val insertion = (installRequests
      returning installRequests.map(_.id)
      into ((request, id) => request.copy(id = Some(id)))) += installRequest
    db.run(insertion)
  }
}
