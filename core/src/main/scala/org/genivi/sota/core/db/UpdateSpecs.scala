/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID

import eu.timepit.refined.api.Refined
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.UpdateRequests.UpdateRequestTable
import org.genivi.sota.data.{Device, Namespace, PackageId, Uuid}
import org.genivi.sota.db.SlickExtensions
import java.time.Instant

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.http.Errors.MissingEntity
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

  case class UpdateSpecRow(requestId: UUID, device: Uuid, status: UpdateStatus, installPos: Int,
                           createdAt: Instant, updatedAt: Instant) {
    def toUpdateSpec(request: UpdateRequest, dependencies: Set[Package]) =
      UpdateSpec(request, device, status, dependencies, installPos, createdAt, updatedAt)
  }

  // scalastyle:off
  /**
   * Each row corresponds to an [[UpdateSpec]] instance except that `dependencies` are kept in [[RequiredPackageTable]]
   */
  class UpdateSpecTable(tag: Tag) extends Table[UpdateSpecRow](tag, "UpdateSpec") {

    def requestId = column[UUID]("update_request_id")
    def device = column[Uuid]("device_uuid")
    def status = column[UpdateStatus]("status")
    def installPos = column[Int]("install_pos")
    def creationTime = column[Instant]("creation_time")
    def updateTime = column[Instant]("update_time")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    // given `id` is already unique across namespaces, no need to include namespace.
    def pk = primaryKey("pk_update_specs", (requestId, device))

    def * = (requestId, device, status, installPos, creationTime, updateTime) <> (UpdateSpecRow.tupled, UpdateSpecRow.unapply)
  }
  // scalastyle:on

  // scalastyle:off
  /**
   * Child table of [[UpdateSpecTable]]. For each row in that table there's one or more rows in this table.
   */
  class RequiredPackageTable(tag: Tag)
      extends Table[(UUID, Uuid, UUID)](tag, "RequiredPackage") {
    def requestId = column[UUID]("update_request_id")
    def device = column[Uuid]("device_uuid")
    def packageUuid = column[UUID]("package_uuid")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_downloads", (requestId, device, packageUuid))

    def * = (requestId, device, packageUuid)
  }
  // scalastyle:on

  val updateSpecs = TableQuery[UpdateSpecTable]

  val requiredPackages = TableQuery[RequiredPackageTable]

  val updateRequests = TableQuery[UpdateRequestTable]

  case class MiniUpdateSpec(requestId: UUID,
                            requestSignature: String,
                            device: Uuid,
                            deps: Queue[Package])

  /**
    * Add an update for a specific device.
    * This update will consist of one-or-more packages that need to be installed
    * on a single device
    *
    * @param updateSpec The list of packages that should be installed
    */
  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val updateSpecRow = UpdateSpecRow(updateSpec.request.id, updateSpec.device,
      updateSpec.status, updateSpec.installPos, updateSpec.creationTime, updateSpec.updateTime)

    def dependencyProjection(p: Package) = (updateSpec.request.id, updateSpec.device, p.uuid)

    DBIO.seq(
      updateSpecs += updateSpecRow,
      requiredPackages ++= updateSpec.dependencies.map( dependencyProjection )
    )
  }

  /**
    * Reusable sub-query, PK lookup in [[UpdateSpecTable]] table.
    */
  private def queryBy(device: Uuid, requestId: UUID): Query[UpdateSpecTable, UpdateSpecRow, Seq] =
  updateSpecs.filter(row => row.device === device && row.requestId === requestId)

  /**
    * Lookup by PK in [[UpdateSpecTable]] table.
    * Note: A tuple is returned instead of an [[UpdateSpec]] instance because
    * the later would require joining [[RequiredPackageTable]] to populate the `dependencies` of that instance.
    */
  def findBy(arg: UpdateSpec): DBIO[UpdateSpecRow] = {
    queryBy(arg.device, arg.request.id).result.head
  }

  /**
    * Lookup by PK in [[UpdateSpecTable]] table.
    * Note: A tuple is returned instead of an [[UpdateSpec]] instance because
    * the later would require joining [[RequiredPackageTable]] to populate the `dependencies` of that instance.
    */
  def findBy(device: Uuid, requestId: Refined[String, Uuid.Valid]): DBIO[UpdateSpecRow] = {
    queryBy(device, UUID.fromString(requestId.get)).result.head
  }

  case class UpdateSpecPackages(miniUpdateSpec: MiniUpdateSpec, packages: Queue[Package])

  // scalastyle:off cyclomatic.complexity

  def load(device: Uuid, updateId: UUID)
          (implicit ec: ExecutionContext) : DBIO[Option[MiniUpdateSpec]] = {
    val q = for {
      r  <- updateRequests if r.id === updateId
      us <- updateSpecs if us.device === device && us.requestId === r.id
      rp <- requiredPackages if rp.device === device && rp.requestId === updateId
      p  <- Packages.packages if p.uuid === rp.packageUuid
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
    * @param pkgId The package to search for
    * @return A list of devices that the package will be installed on
    */
  def getDevicesQueuedForPackage(ns: Namespace, pkgId: PackageId) :
    DBIO[Seq[Uuid]] = {
    val q = for {
      s <- updateSpecs if s.status === UpdateStatus.Pending
      u <- updateRequests if s.requestId === u.id
      pkg <- Packages.packages if pkg.namespace === ns && pkg.name === pkgId.name && pkg.version === pkgId.version &&
      u.packageUuid === pkg.uuid
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
  def setStatus(device: Uuid, updateRequestId: UUID, newStatus: UpdateStatus): DBIO[Int] = {
    updateSpecs
      .filter(row => row.device === device && row.requestId === updateRequestId)
      .map(_.status)
      .update(newStatus)
      .transactionally
  }

  /**
    * Abort a pending [[UpdateSpec]] specified as (device, [[UpdateRequest]]) combination.
    * Only an update with status 'Pending' is aborted.
    *
    * @param uuid of the [[UpdateRequest]] being cancelled for the given device
    */
  def cancelUpdate(device: Uuid, uuid: Refined[String, Uuid.Valid])(implicit ec: ExecutionContext): DBIO[Unit] = {
    updateSpecs
      .filter(us => us.device === device && us.requestId === uuid && us.status === UpdateStatus.Pending)
      .map(_.status)
      .update(UpdateStatus.Canceled)
      .handleSingleUpdateError(MissingEntity(classOf[UpdateSpec]))
  }

  def cancelAllUpdatesBy(status: UpdateStatus, namespace: Namespace, packageId: PackageId)
                        (implicit ec: ExecutionContext): DBIO[Int] = {
    val updateRequestIdsQuery = for {
      us <- updateSpecs if us.status === status
      ur <- updateRequests if ur.id === us.requestId
      pkg <- Packages.packages if pkg.uuid === ur.packageUuid &&
      pkg.namespace === namespace && pkg.name === packageId.name && pkg.version === packageId.version
    } yield ur.id

    val createHistoriesIO = InstallHistories.logAll(updateRequestIdsQuery, success = false)

    // Slick does not allow us to use `in` instead of `inSet`, so we need to use DBIO instead of updateRequestIdsQuery
    val dbIO = updateRequestIdsQuery.result.flatMap { ids =>
      updateSpecs.filter(_.requestId.inSet(ids)).map(_.status).update(UpdateStatus.Canceled)
    }

    createHistoriesIO.andThen(dbIO).transactionally
  }


  /**
    * The [[UpdateSpec]]-s (excluding dependencies but including status) for the given [[UpdateRequest]].
    * Each element in the result corresponds to a different device.
    */
  def listUpdatesById(updateRequestId: Refined[String, Uuid.Valid]): DBIO[Seq[UpdateSpecRow]] =
    updateSpecs.filter(s => s.requestId === UUID.fromString(updateRequestId.get)).result
}
