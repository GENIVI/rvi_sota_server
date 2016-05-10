/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID
import org.genivi.sota.core.data.OperationResult
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
    def id = column[String]("id", O.PrimaryKey)
    def updateId = column[UUID]("update_request_id")
    def resultCode = column[Int]("result_code")
    def resultText = column[String]("result_text")
    // scalastyle:on

    import shapeless._

    // scalastyle:off public.methods.have.type
    // scalastyle:off method.name
    def * = (id, updateId, resultCode, resultText).shaped <>
      (x => OperationResult(x._1, x._2, x._3, x._4),
      (x: OperationResult) => Some((x.id, x.updateId, x.resultCode, x.resultText)))
    // scalastyle:on
  }

  /**
   * Internal helper definition to accesss the SQL table
   */
  val all = TableQuery[OperationResultTable]

  /**
   * List all the update results that have been ever created
   * @return A list of operation results
   */
  def list: DBIO[Seq[OperationResult]] = all.result

  /**
   * Add a new package update. Package updated specify a specific package at a
   * specific version to be installed in a time window, with a given priority
   * @param request A new update request to add
   */
  def persist(result: OperationResult)
             (implicit ec: ExecutionContext): DBIO[OperationResult] = (all += result).map( _ => result)

}
