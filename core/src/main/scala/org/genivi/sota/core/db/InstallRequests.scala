package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.InstallRequest

object InstallRequests extends DatabaseConfig {

  class InstallRequestsTable(tag: Tag) extends Table[InstallRequest](tag, "InstallRequest") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def packageId = column[Long]("packageId")
    def priority = column[Int]("priority")
    def startAfter = column[DateTime]("startAfter")
    def endBefore = column[DateTime]("endBefore")

    def * = (id.?, packageId, priority, startAfter, endBefore) <>
      ((InstallRequest.apply _).tupled, InstallRequest.unapply)
  }

  val installRequests = TableQuery[InstallRequestsTable]

  def list: Future[Seq[InstallRequest]] = db.run(installRequests.result)

  def create(installRequest: InstallRequest): Future[InstallRequest] = {
    val insertion = (installRequests
      returning installRequests.map(_.id)
      into ((request, id) => request.copy(id = Some(id)))) += installRequest
    db.run(insertion)
  }
}
