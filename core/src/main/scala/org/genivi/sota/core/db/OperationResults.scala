/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import eu.timepit.refined.api.Refined
import java.util.UUID

import org.genivi.sota.core.data.OperationResult
import org.genivi.sota.data.{Namespace, Uuid}

import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api._
import java.time.Instant

/**
 * Database mapping definition for the OperationResults table.
 * These refer to the results of an UpdateRequest operation.
 */
object OperationResults {

  import org.genivi.sota.db._
  import SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  /**
   * Slick mapping definition for the UpdateRequests table
   * @see {@link http://slick.typesafe.com/}
   */
  class OperationResultTable(tag: Tag) extends Table[OperationResult](tag, "OperationResult") {
    def id          = column[String]("id")
    def updateId    = column[UUID]("update_request_id")
    def resultCode  = column[Int]("result_code")
    def resultText  = column[String]("result_text")
    def device      = column[Uuid]("device_uuid")
    def namespace   = column[Namespace]("namespace")
    def receivedAt  = column[Instant]("received_at")

    def pk = primaryKey("pk_OperationResultTable", id)

    def * = (id, updateId, resultCode, resultText, device, receivedAt).shaped <>
      (x => OperationResult(x._1, x._2, x._3, x._4, x._5, x._6),
      (x: OperationResult) => Some((x.id, x.updateId, x.resultCode, x.resultText, x.device, x.receivedAt)))
  }
  // scalastyle:on

  /**
   * Internal helper definition to access the SQL table
   */
  val all = TableQuery[OperationResultTable]

  /**
   * All [[OperationResult]]-s that have been ever created
   */
  def list: DBIO[Seq[OperationResult]] = all.result

  /**
   * All [[OperationResult]]-s for the given [[UpdateRequest]]
   */
  def byId(id: Refined[String, Uuid.Valid])(implicit ec: ExecutionContext): DBIO[Seq[OperationResult]] =
    all.filter(r => r.updateId === UUID.fromString(id.get)).result

  /**
    * All [[OperationResult]]-s for the given device.
    */
  def byDevice(device: Uuid)(implicit ec: ExecutionContext): DBIO[Seq[OperationResult]] =
    all.filter(_.device === device).result

  /**
    * All [[OperationResult]]-s for the given (device, [[UpdateRequest]]) combination
    */
  def byDeviceIdAndId(device: Uuid, id: Refined[String, Uuid.Valid])
                     (implicit ec: ExecutionContext): DBIO[Seq[OperationResult]] =
      all.filter(r => r.device === device && r.updateId === UUID.fromString(id.get)).result

  /**
   * Add a new package update. Package updated specify a specific package at a
   * specific version to be installed in a time window, with a given priority
   * @param result A new operation result to add
   */
  def persist(result: OperationResult)
             (implicit ec: ExecutionContext): DBIO[OperationResult] = (all += result).map(_ => result)

}
