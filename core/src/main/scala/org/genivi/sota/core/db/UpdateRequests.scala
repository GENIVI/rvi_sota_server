/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID

import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.data.PackageId
import org.joda.time.DateTime
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

/**
 * Database mapping definition for the UpdateRequests table.
 * These refer to a single software package that should be installed as part
 * of a install campaign.  There one of these shared among multiple VINs: the
 * UpdateSpecs table records the result of the individual install for each
 * vehicle.
 */
object UpdateRequests {

  import org.genivi.sota.db._
  import SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  /**
   * Slick mapping definition for the UpdateRequests table
   * @see [[http://slick.typesafe.com/]]
   */
  class UpdateRequestTable(tag: Tag) extends Table[UpdateRequest](tag, "UpdateRequest") {
    def id = column[UUID]("update_request_id", O.PrimaryKey)
    def packageName = column[PackageId.Name]("package_name")
    def packageVersion = column[PackageId.Version]("package_version")
    def creationTime = column[DateTime]("creation_time")
    def startAfter = column[DateTime]("start_after")
    def finishBefore = column[DateTime]("finish_before")
    def priority = column[Int]("priority")
    def signature = column[String]("signature")
    def description = column[String]("description")
    def requestConfirmation = column[Boolean]("request_confirmation")

    import com.github.nscala_time.time.Imports._
    import shapeless._

    implicit val IntervalGen : Generic[Interval] = new Generic[Interval] {
      type Repr = DateTime :: DateTime :: HNil

      override def to(x : Interval) : Repr = x.start :: x.end :: HNil

      override def from( repr : Repr) : Interval = repr match {
        case start :: end :: HNil => start to end
      }
    }

    def * = (id, packageName, packageVersion, creationTime, startAfter, finishBefore,
             priority, signature, description.?, requestConfirmation).shaped <>
      (x => UpdateRequest(x._1, PackageId(x._2, x._3), x._4, x._5 to x._6, x._7, x._8, x._9, x._10),
      (x: UpdateRequest) => Some((x.id, x.packageId.name, x.packageId.version, x.creationTime,
                                  x.periodOfValidity.start, x.periodOfValidity.end, x.priority,
                                  x.signature, x.description, x.requestConfirmation)))


  }

  /**
   * Internal helper definition to accesss the SQL table
   */
  val all = TableQuery[UpdateRequestTable]

  /**
   * List all the package updates that have been ever created
   * @return A list of update requests
   */
  def list: DBIO[Seq[UpdateRequest]] = all.result

  def byId(updateId: UUID) : DBIO[Option[UpdateRequest]] =
    all.filter {_.id === updateId }.result.headOption

  /**
   * Add a new package update. Package updated specify a specific package at a
   * specific version to be installed in a time window, with a given priority
   * @param request A new update request to add
   */
  def persist(request: UpdateRequest)
             (implicit ec: ExecutionContext): DBIO[Unit] = (all += request).map( _ => ())
}
