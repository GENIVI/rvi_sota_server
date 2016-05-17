/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import java.util.UUID

import org.genivi.sota.core.data.OperationResult
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.Vehicle

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._

/**
 * Database mapping definition for the OperationResults table.
 * These refer to the results of an UpdateRequest operation.
 */
object OperationResults {

  import org.genivi.sota.db._
  import SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  /**
   * Slick mapping definition for the UpdateRequests table
   * @see {@link http://slick.typesafe.com/}
   */
  class OperationResultTable(tag: Tag) extends Table[OperationResult](tag, "OperationResult") {
    // scalastyle:off public.methods.have.type
    def id          = column[String]("id", O.PrimaryKey)
    def updateId    = column[UUID]("update_request_id")
    def resultCode  = column[Int]("result_code")
    def resultText  = column[String]("result_text")
    def vin         = column[Vehicle.Vin]("vin")
    def namespace   = column[Namespace]("namespace")
    // scalastyle:on

    import shapeless._

    // scalastyle:off public.methods.have.type
    // scalastyle:off method.name
    def * = (id, updateId, resultCode, resultText, vin, namespace).shaped <>
      (x => OperationResult(x._1, x._2, x._3, x._4, x._5, x._6),
      (x: OperationResult) => Some((x.id, x.updateId, x.resultCode, x.resultText, x.vin, x.namespace)))
    // scalastyle:on
  }

  /**
   * Internal helper definition to access the SQL table
   */
  val all = TableQuery[OperationResultTable]

  /**
   * List all the update results that have been ever created
   * @return A list of operation results
   */
  def list: DBIO[Seq[OperationResult]] = all.result

  /**
   * List all the update results for a give update ID
   * @param ns The namespace of the user making the query
   * @param id the uuid of the Update that operation results should be fetched for
   * @return all OperationResults associated with the given id
   */
  def byId(ns: Namespace, id: Refined[String, Uuid])(implicit ec: ExecutionContext): DBIO[Seq[OperationResult]] =
    all.filter(r => r.namespace === ns && r.updateId === UUID.fromString(id.get)).result

  /**
    * Get all OperationResults associated with the given VIN
    * @param ns The namespace of the user making the query
    * @param vin The VIN for which all OperationResults should be returned
    * @return all OperationResults associated with the given VIN
    */
  def byVin(ns: Namespace, vin: Vehicle.Vin)(implicit ec: ExecutionContext): DBIO[Seq[OperationResult]] =
    all.filter(r => r.namespace === ns && r.vin === vin).result

  /**
   * Add a new package update. Package updated specify a specific package at a
   * specific version to be installed in a time window, with a given priority
   * @param result A new operation result to add
   */
  def persist(result: OperationResult)
             (implicit ec: ExecutionContext): DBIO[OperationResult] = (all += result).map(_ => result)

}
