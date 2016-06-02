/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.UpdateRequests.UpdateRequestTable
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{Device, PackageId, Vehicle}
import org.genivi.sota.db.SlickExtensions
import java.time.Instant

import org.genivi.sota.core.Errors
import slick.driver.MySQLDriver.api._

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext

/**
  * Database mapping definitions for the UpdateSpecs and RequiredPackages tables.
  * <ul>
  *   <li>A row in UpdateSpec records the status of a package install for a specific device.</li>
  *   <li>For each such row, one or more rows in RequiredPackage indicate the individual packages
  *       (including dependencies) that need to be installed.</li>
  * </ul>
  */
object UpdateSpecs {

  import SlickExtensions._
  import UpdateStatus._
  import org.genivi.sota.refined.SlickRefined._

  implicit val UpdateStatusColumn = MappedColumnType.base[UpdateStatus, String](_.value.toString, UpdateStatus.withName)

  type UpdateSpecTableRowType = (Namespace, UUID, Device.Id, UpdateStatus, Int, Instant)

  // scalastyle:off
  /**
   * Each row corresponds to an [[UpdateSpec]] instance except that `dependencies` are kept in [[RequiredPackageTable]]
   */
  class UpdateSpecTable(tag: Tag)
      extends Table[UpdateSpecTableRowType](tag, "UpdateSpec") {

    def namespace = column[Namespace]("namespace")
    def requestId = column[UUID]("update_request_id")
    def device = column[Device.Id]("device_uuid")
    def status = column[UpdateStatus]("status")
    def installPos = column[Int]("install_pos")
    def creationTime = column[Instant]("creation_time")


    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    // given `id` is already unique across namespaces, no need to include namespace.
    def pk = primaryKey("pk_update_specs", (requestId, device))

    def * = (namespace, requestId, device, status, installPos, creationTime)
  }
  // scalastyle:on

  // scalastyle:off
  /**
   * Child table of [[UpdateSpecTable]]. For each row in that table there's one or more rows in this table.
   */
  class RequiredPackageTable(tag: Tag)
      extends Table[(Namespace, UUID, Device.Id, PackageId.Name, PackageId.Version)](tag, "RequiredPackage") {
    def namespace = column[Namespace]("namespace")
    def requestId = column[UUID]("update_request_id")
    def device = column[Device.Id]("device_uuid")
    def packageName = column[PackageId.Name]("package_name")
    def packageVersion = column[PackageId.Version]("package_version")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_downloads", (namespace, requestId, device, packageName, packageVersion))

    def * = (namespace, requestId, device, packageName, packageVersion)
  }
  // scalastyle:on

  val updateSpecs = TableQuery[UpdateSpecTable]

  val requiredPackages = TableQuery[RequiredPackageTable]

  val updateRequests = TableQuery[UpdateRequestTable]

  case class MiniUpdateSpec(requestId: UUID,
                            requestSignature: String,
                            device: Device.Id,
                            deps: Queue[Package])

  /**
    * Add an update for a specific device.
    * This update will consist of one-or-more packages that need to be installed
    * on a single device
    *
    * @param updateSpec The list of packages that should be installed
    */
  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val specProjection = (
      updateSpec.namespace, updateSpec.request.id, updateSpec.device,
      updateSpec.status, updateSpec.installPos, updateSpec.creationTime)

    def dependencyProjection(p: Package) =
      // TODO: we're taking the namespace of the update spec, not necessarily the namespace of the package!
      (updateSpec.namespace, updateSpec.request.id, updateSpec.device, p.id.name, p.id.version)

    DBIO.seq(
      updateSpecs += specProjection,
      requiredPackages ++= updateSpec.dependencies.map( dependencyProjection )
    )
  }

  /**
    * Reusable sub-query, PK lookup in [[UpdateSpecTable]] table.
    */
  private def queryBy(namespace: Namespace,
                      deviceId: Device.Id,
                      requestId: UUID): Query[UpdateSpecTable, UpdateSpecTableRowType, Seq] = {
    updateSpecs
      .filter(row => row.namespace === namespace && row.device === deviceId && row.requestId === requestId)
  }

  /**
    * Lookup by PK in [[UpdateSpecTable]] table.
    * Note: A tuple is returned instead of an [[UpdateSpec]] instance because
    * the later would require joining [[RequiredPackageTable]] to populate the `dependencies` of that instance.
    */
  def findBy(arg: UpdateSpec): DBIO[UpdateSpecTableRowType] = {
    queryBy(arg.namespace, arg.device, arg.request.id).result.head
  }

  /**
    * Lookup by PK in [[UpdateSpecTable]] table.
    * Note: A tuple is returned instead of an [[UpdateSpec]] instance because
    * the later would require joining [[RequiredPackageTable]] to populate the `dependencies` of that instance.
    */
  def findBy(namespace: Namespace, device: Device.Id,
             requestId: Refined[String, Uuid]): DBIO[UpdateSpecTableRowType] = {
    queryBy(namespace, device, UUID.fromString(requestId.get)).result.head
  }

  case class UpdateSpecPackages(miniUpdateSpec: MiniUpdateSpec, packages: Queue[Package])

  // scalastyle:off cyclomatic.complexity
  /**
    * Fetch from DB zero or one [[UpdateSpec]] for the given combination ([[UpdateRequest]], device)
    *
    * @param deviceId The device to install on
    * @param updateId Id of the [[UpdateRequest]] to install
    */
  def load(device: Device.Id, updateId: UUID)
          (implicit ec: ExecutionContext) : DBIO[Option[MiniUpdateSpec]] = {
    val q = for {
      r  <- updateRequests if (r.id === updateId)
      ns  = r.namespace
      us <- updateSpecs if(us.device === device && us.requestId == r.id && us.namespace == r.namespace)
      rp <- requiredPackages if (rp.device === device &&
                                 rp.namespace === ns &&
                                 rp.requestId === updateId)
      p  <- Packages.packages if (p.namespace === ns &&
                                  p.name === rp.packageName &&
                                  p.version === rp.packageVersion)
    } yield (r.signature, p)

    q.result map { rows =>
      rows.headOption.map(_._1).map { sig =>
        val paks = rows.map(_._2).to[Queue]
        MiniUpdateSpec(updateId, sig, device, paks)
      }
    }
  }
  // scalastyle:on

  /**
    * Return a list of all the devices that a specific version of a package will be
    * installed on.  Note that devices where the package has started installation,
    * or has either been installed or where the install failed are not included.
    *
    * @param pkgName The package name to search for
    * @param pkgVer The version of the package to search for
    * @return A list of devices that the package will be installed on
    */
  def getDevicesQueuedForPackage(ns: Namespace, pkgName: PackageId.Name, pkgVer: PackageId.Version) :
    DBIO[Seq[Device.Id]] = {
    val specs = updateSpecs.filter(r => r.namespace === ns && r.status === UpdateStatus.Pending)
    val q = for {
      s <- specs
      u <- updateRequests if (s.requestId === u.id) && (u.packageName === pkgName) && (u.packageVersion === pkgVer)
    } yield s.device
    q.result
  }

  /**
    * Rewrite in the DB the status of an [[UpdateSpec]], ie for a ([[UpdateRequest]], device) combination.
    */
  def setStatus(spec: UpdateSpec, newStatus: UpdateStatus) : DBIO[Int] = {
    setStatus(spec.device, spec.request.id, newStatus)
  }

  /**
    * Rewrite in the DB the status of an [[UpdateSpec]], ie for a ([[UpdateRequest]], device) combination.
    */
  def setStatus(device: Device.Id, updateRequestId: UUID, newStatus: UpdateStatus): DBIO[Int] = {
    updateSpecs
      .filter(row => row.device === device && row.requestId === updateRequestId)
      .map(_.status)
      .update(newStatus)
      .transactionally
  }

  /**
    * Abort a pending [[UpdateSpec]] specified as ([[UpdateRequest]], VIN).
    * Only an update with status 'Pending' is aborted.
    *
    * @param uuid of the [[UpdateRequest]] being cancelled for the given VIN
    */
  def cancelUpdate(device: Device.Id, uuid: Refined[String, Uuid])(implicit ec: ExecutionContext): DBIO[Int] = {
    updateSpecs
      .filter(us => us.device === device && us.requestId === uuid && us.status === UpdateStatus.Pending)
      .map(_.status)
      .update(UpdateStatus.Canceled)
      .flatMap { rowsAffected =>
        if (rowsAffected == 1) {
          DBIO.successful(rowsAffected)
        } else {
          DBIO.failed(Errors.MissingUpdateSpec)
        }
      }
  }

  /**
    * The [[UpdateSpec]]-s (excluding dependencies but including status) for the given [[UpdateRequest]].
    * Each element in the result corresponds to a different device.
    */
  def listUpdatesById(updateRequestId: Refined[String, Uuid]): DBIO[Seq[UpdateSpecTableRowType]] =
    updateSpecs.filter(s => s.requestId === UUID.fromString(updateRequestId.get)).result

  /**
    * Delete all the updates for a specific device
    * This is part of the process for deleting a device from the system
    *
    * @param device The device to get the device to delete from
    */
  def deleteUpdateSpecByDevice(ns: Namespace, device: Device.Id) : DBIO[Int] =
    updateSpecs.filter(s => s.namespace === ns && s.device === device).delete

  /**
    * Delete all the required packages that are needed for a device.
    * This is part of the process for deleting a device from the system
    *
    * @param device The device to get the device to delete from
    */
  def deleteRequiredPackageByDevice(ns: Namespace, device: Device.Id) : DBIO[Int] =
    requiredPackages.filter(rp => rp.namespace === ns && rp.device === device).delete
}
