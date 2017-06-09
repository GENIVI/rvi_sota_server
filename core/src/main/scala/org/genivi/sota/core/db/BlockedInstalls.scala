/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.db

import org.genivi.sota.data.Uuid
import org.genivi.sota.core.data.BlockedInstall
import org.genivi.sota.refined.SlickRefined._
import java.time.Instant

import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api._


object BlockedInstalls {

  import org.genivi.sota.db.SlickExtensions._

  // scalastyle:off
  class BlockedInstallTable(tag: Tag) extends Table[BlockedInstall](tag, "BlockedInstall") {
    def id = column[Uuid]("uuid")
    def blockedAt = column[Instant]("blocked_at")

    def * = (id, blockedAt).shaped <>
      ((BlockedInstall.apply _).tupled, BlockedInstall.unapply)

    def pk = primaryKey("id", id)
  }
  // scalastyle:on
  val all = TableQuery[BlockedInstallTable]

  def get(id: Uuid)
         (implicit ec: ExecutionContext): DBIO[Seq[BlockedInstall]] = {
    all.filter(d => d.id === id).result
  }

  def delete(id: Uuid)
            (implicit ec: ExecutionContext): DBIO[Int] = {
    all
      .filter(d => d.id === id)
      .delete
  }

  def persist(id: Uuid)
             (implicit ec: ExecutionContext): DBIO[Int] = {
    all.insertOrUpdate(
      BlockedInstall.from(id)
    )
  }

  def isBlockedInstall(id: Uuid)
                      (implicit ec: ExecutionContext): DBIO[Boolean] = {
    all
      .filter(d => d.id === id)
      .exists
      .result
  }

}
