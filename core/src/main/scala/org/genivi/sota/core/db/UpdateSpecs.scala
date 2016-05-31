/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID

import akka.http.scaladsl.model.Uri
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.UpdateRequests.UpdateRequestTable
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.Vehicle.Vin
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.db.SlickExtensions
import slick.driver.MySQLDriver.api._

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext

/**
  * Database mapping definitions for the UpdateSpecs and RequiredPackages tables.
  * <ul>
  *   <li>A row in UpdateSpec records the status of a package install for a specific VIN.</li>
  *   <li>For each such row, one or more rows in RequiredPackage indicate the individual packages
  *       (including dependencies) that need to be installed.</li>
  * </ul>
  */
object UpdateSpecs {

  import SlickExtensions._
  import UpdateStatus._
  import org.genivi.sota.refined.SlickRefined._

  implicit val UpdateStatusColumn = MappedColumnType.base[UpdateStatus, String](_.value.toString, UpdateStatus.withName)

  type UpdateSpecTableRowType = (Namespace, UUID, Vehicle.Vin, UpdateStatus, Int)

  // scalastyle:off
  /**
   * Each row corresponds to an [[UpdateSpec]] instance except that `dependencies` are kept in [[RequiredPackageTable]]
   */
  class UpdateSpecTable(tag: Tag)
      extends Table[UpdateSpecTableRowType](tag, "UpdateSpec") {
    def namespace = column[Namespace]("namespace")
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.Vin]("vin")
    def status = column[UpdateStatus]("status")
    def installPos = column[Int]("install_pos")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    // given `id` is already unique across namespaces, no need to include namespace.
    def pk = primaryKey("pk_update_specs", (requestId, vin))

    def * = (namespace, requestId, vin, status, installPos)
  }
  // scalastyle:on

  // scalastyle:off
  /**
   * Child table of [[UpdateSpecTable]]. For each row in that table there's one or more rows in this table.
   */
  class RequiredPackageTable(tag: Tag)
      extends Table[(Namespace, UUID, Vehicle.Vin, PackageId.Name, PackageId.Version)](tag, "RequiredPackage") {
    def namespace = column[Namespace]("namespace")
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.Vin]("vin")
    def packageName = column[PackageId.Name]("package_name")
    def packageVersion = column[PackageId.Version]("package_version")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_downloads", (namespace, requestId, vin, packageName, packageVersion))

    def * = (namespace, requestId, vin, packageName, packageVersion)
  }
  // scalastyle:on

  val updateSpecs = TableQuery[UpdateSpecTable]

  val requiredPackages = TableQuery[RequiredPackageTable]

  val updateRequests = TableQuery[UpdateRequestTable]

  case class MiniUpdateSpec(requestId: UUID,
                            requestSignature: String,
                            vin: Vehicle.Vin)

  /**
    * Add an update for a specific VIN.
    * This update will consist of one-or-more packages that need to be installed
    * on a single VIN
    *
    * @param updateSpec The list of packages that should be installed
    */
  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val specProjection = (
      updateSpec.namespace, updateSpec.request.id, updateSpec.vin,  updateSpec.status, updateSpec.installPos)

    def dependencyProjection(p: Package) =
      // TODO: we're taking the namespace of the update spec, not necessarily the namespace of the package!
      (updateSpec.namespace, updateSpec.request.id, updateSpec.vin, p.id.name, p.id.version)

    DBIO.seq(
      updateSpecs += specProjection,
      requiredPackages ++= updateSpec.dependencies.map( dependencyProjection )
    )
  }

  /**
    * Reusable sub-query, PK lookup in [[UpdateSpecTable]] table.
    */
  private def queryBy(arg: UpdateSpec): Query[UpdateSpecTable, UpdateSpecTableRowType, Seq] = {
    updateSpecs
      .filter(row => row.namespace === arg.namespace && row.vin === arg.vin && row.requestId === arg.request.id)
  }

  /**
    * Lookup by PK in [[UpdateSpecTable]] table.
    * Note: A tuple is returned instead of an [[UpdateSpec]] instance because
    * the later would require joining [[RequiredPackageTable]] to populate the `dependencies` of that instance.
    */
  def findBy(arg: UpdateSpec): DBIO[UpdateSpecTableRowType] = {
    queryBy(arg).result.head
  }

  // scalastyle:off cyclomatic.complexity
  /**
    * Fetch from DB zero or one [[UpdateSpec]] for the given combination ([[UpdateRequest]], VIN)
    *
    * @param vin The VIN to install on
    * @param updateId Id of the [[UpdateRequest]] to install
    */
  def load(vin: Vehicle.Vin, updateId: UUID)
          (implicit ec: ExecutionContext) : DBIO[Option[(MiniUpdateSpec, Queue[Package])]] = {
    val q = for {
      r  <- updateRequests if (r.id === updateId)
      ns  = r.namespace
      rp <- requiredPackages if (rp.vin === vin &&
                                 rp.namespace === ns &&
                                 rp.requestId === updateId)
      p  <- Packages.packages if (p.namespace === ns &&
                                  p.name === rp.packageName &&
                                  p.version === rp.packageVersion)
    } yield (r.signature, p)

    val qres: DBIO[Seq[(String, Package)]] = q.result

    qres map { rows =>
      if (rows.isEmpty) {
        None
      } else {
        val requestSignature = rows.head._1
        val paks = for (row <- rows; p = row._2) yield p
        Some(Tuple2(MiniUpdateSpec(updateId, requestSignature, vin), paks.toSet.to[Queue]))
      }
    }

  }
  // scalastyle:on

  /**
    * Return a list of all the VINs that a specific version of a package will be
    * installed on.  Note that VINs where the package has started installation,
    * or has either been installed or where the install failed are not included.
    *
    * @param pkgName The package name to search for
    * @param pkgVer The version of the package to search for
    * @return A list of VINs that the package will be installed on
    */
  def getVinsQueuedForPackage(ns: Namespace, pkgName: PackageId.Name, pkgVer: PackageId.Version) :
    DBIO[Seq[Vehicle.Vin]] = {
    val specs = updateSpecs.filter(r => r.namespace === ns && r.status === UpdateStatus.Pending)
    val q = for {
      s <- specs
      u <- updateRequests if (s.requestId === u.id) && (u.packageName === pkgName) && (u.packageVersion === pkgVer)
    } yield s.vin
    q.result
  }

  /**
    * Rewrite in the DB the status of an [[UpdateSpec]], ie for a ([[UpdateRequest]], VIN) combination.
    */
  def setStatus(spec: UpdateSpec, newStatus: UpdateStatus) : DBIO[Int] = {
    setStatus(spec.vin, spec.request.id, newStatus)
  }

  /**
    * Rewrite in the DB the status of an [[UpdateSpec]], ie for a ([[UpdateRequest]], VIN) combination.
    */
  def setStatus(vin: Vehicle.Vin, updateRequestId: UUID, newStatus: UpdateStatus): DBIO[Int] = {
    updateSpecs
      .filter(row => row.vin === vin && row.requestId === updateRequestId)
      .map(_.status)
      .update(newStatus)
      .transactionally
  }

  /**
    * Abort a pending update specified by uuid and vin. Updates with statuses other than 'Pending' will not be aborted
    */
  def cancelUpdate(vin: Vehicle.Vin, uuid: Refined[String, Uuid]): DBIO[Int] = {
    updateSpecs
      .filter(us => us.vin === vin && us.requestId === uuid && us.status === UpdateStatus.Pending)
      .map(_.status)
      .update(UpdateStatus.Canceled)
      .transactionally
  }

  /**
    * The [[UpdateSpec]]-s (excluding dependencies but including status) for the given [[UpdateRequest]].
    * Each element in the result corresponds to a different VIN.
    */
  def listUpdatesById(updateRequestId: Refined[String, Uuid]): DBIO[Seq[UpdateSpecTableRowType]] =
    updateSpecs.filter(s => s.requestId === UUID.fromString(updateRequestId.get)).result

  /**
    * Delete all the updates for a specific VIN
    * This is part of the process for deleting a VIN from the system
    *
    * @param vehicle The vehicle to get the VIN to delete from
    */
  def deleteUpdateSpecByVin(ns: Namespace, vehicle: Vehicle) : DBIO[Int] =
    updateSpecs.filter(s => s.namespace === ns && s.vin === vehicle.vin).delete

  /**
    * Delete all the required packages that are needed for a VIN.
    * This is part of the process for deleting a VIN from the system
    *
    * @param vehicle The vehicle to get the VIN to delete from
    */
  def deleteRequiredPackageByVin(ns: Namespace, vehicle : Vehicle) : DBIO[Int] =
    requiredPackages.filter(rp => rp.namespace === ns && rp.vin === vehicle.vin).delete
}
