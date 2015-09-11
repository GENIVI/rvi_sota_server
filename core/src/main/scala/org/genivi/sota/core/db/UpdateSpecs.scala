/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.util.UUID
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.data.{UpdateSpec, Download, Vehicle, UpdateStatus, Package}

object UpdateSpecs {

  import org.genivi.sota.refined.SlickRefined._
  import UpdateStatus._

  implicit val UpdateStatusColumn = MappedColumnType.base[UpdateStatus, String]( _.value.toString, UpdateStatus.withName )

  class UpdateSpecsTable(tag: Tag) extends Table[(UUID, Vehicle.IdentificationNumber, UpdateStatus)](tag, "UpdateSpecs") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def status = column[UpdateStatus]("status")

    def pk = primaryKey("pk_update_specs", (requestId, vin))

    def * = (requestId, vin, status)
  }

  class RequiredPackagesTable(tag: Tag) extends Table[(UUID, Vehicle.IdentificationNumber, Package.Name, Package.Version)](tag, "RequiredPackages") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def packageName = column[Package.Name]("package_name")
    def packageVersion = column[Package.Version]("package_version")

    def pk = primaryKey("pk_downloads", (requestId, vin, packageName, packageVersion))

    def * = (requestId, vin, packageName, packageVersion)
  }

  val updateSpecs = TableQuery[UpdateSpecsTable]

  val requiredPackages = TableQuery[RequiredPackagesTable]

  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val specProjection = (updateSpec.request.id, updateSpec.vin,  updateSpec.status)

    def dependencyProjection(p: Package) =
      (updateSpec.request.id, updateSpec.vin, p.id.name, p.id.version)

    DBIO.seq(
      updateSpecs += specProjection,
      requiredPackages ++= updateSpec.dependencies.map( dependencyProjection )
    )
  }

}
