/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID

import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.data.Namespace
import slick.jdbc.MySQLProfile.api._
import java.time.Instant

import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.data.Interval

import scala.concurrent.ExecutionContext

/**
 * Database mapping definition for the UpdateRequests table.
 * These refer to a single software package that should be installed as part
 * of a install campaign.  There one of these shared among multiple devices: the
 * UpdateSpecs table records the result of the individual install for each
 * device.
 */
object UpdateRequests {

  import org.genivi.sota.db._
  import SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  /**
   * Slick mapping definition for the UpdateRequests table
   * @see [[http://slick.typesafe.com/]]
   */
  class UpdateRequestTable(tag: Tag) extends Table[UpdateRequest](tag, "UpdateRequest") {
    def id = column[UUID]("update_request_id")
    def namespace = column[Namespace]("namespace")
    def packageUuid = column[UUID]("package_uuid")
    def creationTime = column[Instant]("creation_time")
    def startAfter = column[Instant]("start_after")
    def finishBefore = column[Instant]("finish_before")
    def priority = column[Int]("priority")
    def signature = column[String]("signature")
    def description = column[String]("description")
    def requestConfirmation = column[Boolean]("request_confirmation")

    import shapeless._

    implicit val IntervalGen : Generic[Interval] = new Generic[Interval] {
      type Repr = Instant :: Instant :: HNil

      override def to(x : Interval) : Repr = x.start :: x.end :: HNil

      override def from( repr : Repr) : Interval = repr match {
        case start :: end :: HNil => Interval(start, end)
      }
    }

    // given `id` is already unique across namespaces, no need to include namespace. Also avoids Slick issue #966.
    def pk = primaryKey("pk_UpdateRequest", id)

    def * = (id, namespace, packageUuid, creationTime, startAfter, finishBefore,
             priority, signature, description.?, requestConfirmation).shaped <>
      (x => UpdateRequest(x._1, x._2, x._3, x._4, Interval(x._5, x._6), x._7, x._8, x._9, x._10),
      (x: UpdateRequest) => Some((x.id, x.namespace, x.packageUuid, x.creationTime,
                                  x.periodOfValidity.start, x.periodOfValidity.end, x.priority,
                                  x.signature, x.description, x.requestConfirmation)))
  }
  // scalastyle:on

  /**
   * Internal helper definition to access the SQL table
   */
  val all = TableQuery[UpdateRequestTable]

  /**
   * List all the package updates that have been ever created
   * @return A list of update requests
   */
  def list(namespace: Namespace): DBIO[Seq[UpdateRequest]] = all.filter(_.namespace === namespace).result

  /**
   * List all the update requests for a give update ID
   */
  def byId(updateId: UUID)(implicit ec: ExecutionContext): DBIO[UpdateRequest] =
    all
      .filter(_.id === updateId)
      .result
      .headOption
      .failIfNone(SotaCoreErrors.MissingUpdateRequest)

  /**
   * Add a new package update. Package updated specify a specific package at a
   * specific version to be installed in a time window, with a given priority
   * @param request A new update request to add
   */
  def persist(request: UpdateRequest)
             (implicit ec: ExecutionContext): DBIO[Unit] = (all += request).map( _ => ())
}
