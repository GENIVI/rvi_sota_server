/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.data.{Package, UpdateSpec, UpdateStatus}
import org.genivi.sota.core.db.UpdateRequests.UpdateRequestTable
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.db.SlickExtensions
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

/**
 * Database mapping definition for the UpdateSpecs and RequiredPackages tables.
 * UpdateSpecs records the status of a package update for a specific VIN. One
 * UpdateSpecs row has several RequiredPackages rows that detail the individual
 * packages that need to be installed.
 */
object UpdateSpecs {

  import SlickExtensions._
  import UpdateStatus._
  import org.genivi.sota.refined.SlickRefined._

  implicit val UpdateStatusColumn = MappedColumnType.base[UpdateStatus, String](_.value.toString, UpdateStatus.withName)

  /**
   * Slick mapping definition for the UpdateSpecs table
   * @see [[http://slick.typesafe.com/]]
   */
  class UpdateSpecTable(tag: Tag)
      extends Table[(UUID, Vehicle.Vin, UpdateStatus)](tag, "UpdateSpec") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.Vin]("vin")
    def status = column[UpdateStatus]("status")

    def pk = primaryKey("pk_update_specs", (requestId, vin))

    def * = (requestId, vin, status)
  }

  /**
   * Slick mapping definition for the RequiredPackage table
   * @see {@link http://slick.typesafe.com/}
   */
  class RequiredPackageTable(tag: Tag)
      extends Table[(UUID, Vehicle.Vin, PackageId.Name, PackageId.Version)](tag, "RequiredPackage") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.Vin]("vin")
    def packageName = column[PackageId.Name]("package_name")
    def packageVersion = column[PackageId.Version]("package_version")

    def pk = primaryKey("pk_downloads", (requestId, vin, packageName, packageVersion))

    def * = (requestId, vin, packageName, packageVersion)
  }

  /**
   * Internal helper definition to accesss the UpdateSpec table
   */
  val updateSpecs = TableQuery[UpdateSpecTable]

  /**
   * Internal helper definition to accesss the RequiredPackages table
   */
  val requiredPackages = TableQuery[RequiredPackageTable]

  /**
   * Internal helper definition to accesss the UpdateRequest table
   */
  val updateRequests = TableQuery[UpdateRequestTable]

  /**
   * Add an update for a specific VIN.
   * This update will consist of one-or-more packages that need to be installed
   * on a single VIN
   * @param updateSpec The list of packages that should be installed
   */
  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val specProjection = (updateSpec.request.id, updateSpec.vin,  updateSpec.status)

    def dependencyProjection(p: Package) =
      (updateSpec.request.id, updateSpec.vin, p.id.name, p.id.version)

    DBIO.seq(
      updateSpecs += specProjection,
      requiredPackages ++= updateSpec.dependencies.map( dependencyProjection )
    )
  }

  /**
   * Install a list of specific packages on a VIN
   * @param vin The VIN to install on
   * @param updateId Update Id of the update to install
   */
  def load(vin: Vehicle.Vin, updateId: UUID)
          (implicit ec: ExecutionContext) : DBIO[Iterable[UpdateSpec]] = {
    val q = for {
      r  <- updateRequests.filter(_.id === updateId)
      s  <- updateSpecs.filter(_.vin === vin) if (r.id === s.requestId)
      rp <- requiredPackages if (rp.vin === vin && rp.requestId === s.requestId)
      p  <- Packages.packages if (p.name === rp.packageName && p.version === rp.packageVersion)
    } yield (r, s.vin, s.status, p)
    q.result.map( _.groupBy(x => (x._1, x._2, x._3) ).map {
      case ((request, vin, status), xs) => UpdateSpec(request, vin, status, xs.map(_._4).toSet )
    })
  }

  /**
   * Records the status of an update on a specific VIN
   * @param spec The combination of VIN and update request to record the status of
   * @param newStatus The latest status of the installation. One of Pending
   *                  InFlight, Canceled, Failed or Finished.
   */
  def setStatus( spec: UpdateSpec, newStatus: UpdateStatus ) : DBIO[Int] = {
    updateSpecs.filter( t => t.vin === spec.vin && t.requestId === spec.request.id)
      .map( _.status )
      .update( newStatus )
  }

  /**
   * Get the packages that are queued for installation on a VIN.
   * @param vin The VIN to query
   * @return A List of package names + versions that are due to be installed.
   */
  def getPackagesQueuedForVin(vin: Vehicle.Vin)
                             (implicit ec: ExecutionContext) : DBIO[Iterable[PackageId]] = {
    val specs = updateSpecs.filter(r => r.vin === vin && (r.status === UpdateStatus.InFlight ||
      r.status === UpdateStatus.Pending))
    val q = for {
      s <- specs
      u <- updateRequests if s.requestId === u.id
    } yield (u.packageName, u.packageVersion)
    q.result.map(_.map {
      case (packageName, packageVersion) => PackageId(packageName, packageVersion)
    })
  }

  /**
   * Return a list of all the VINs that a specific version of a package will be
   * installed on.  Note that VINs where the package has started installation,
   * or has either been installed or where the install failed are not included.
   * @param pkgName The package name to search for
   * @param pkgVer The version of the package to search for
   * @return A list of VINs that the package will be installed on
   */
  def getVinsQueuedForPackage(pkgName: PackageId.Name, pkgVer: PackageId.Version) :
    DBIO[Seq[Vehicle.Vin]] = {
    val specs = updateSpecs.filter(r => r.status === UpdateStatus.Pending)
    val q = for {
      s <- specs
      u <- updateRequests if(s.requestId === u.id) && (u.packageName === pkgName) && (u.packageVersion === pkgVer)
    } yield s.vin
    q.result
  }

  /**
   * Return the status of a specific update
   * @return The update status
   */
  def listUpdatesById(uuid: Refined[String, Uuid]): DBIO[Seq[(UUID, Vehicle.Vin, UpdateStatus)]] =
    updateSpecs.filter(_.requestId === UUID.fromString(uuid.get)).result

  /**
   * Delete all the updates for a specific VIN
   * This is part of the process for deleting a VIN from the system
   * @param vehicle The vehicle to get the VIN to delete from
   */
  def deleteUpdateSpecByVin(vehicle : Vehicle) : DBIO[Int] = updateSpecs.filter(_.vin === vehicle.vin).delete

  /**
   * Delete all the required packages that are needed for a VIN.
   * This is part of the process for deleting a VIN from the system
   * @param vehicle The vehicle to get the VIN to delete from
   */
  def deleteRequiredPackageByVin(vehicle : Vehicle) : DBIO[Int] = requiredPackages.filter(_.vin === vehicle.vin).delete
}
