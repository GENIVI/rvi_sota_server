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

  class UpdateSpecsTable(tag: Tag) extends Table[(UUID, Vehicle.IdentificationNumber, Int, UpdateStatus)](tag, "UpdateSpecs") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def chunkSize = column[Int]("chunk_size")
    def status = column[UpdateStatus]("status")

    def pk = primaryKey("pk_update_specs", (requestId, vin))

    def * = (requestId, vin, chunkSize, status)
  }

  class DownloadsTable(tag: Tag) extends Table[(UUID, Vehicle.IdentificationNumber, Int, Int, Package.Name, Package.Version)](tag, "Downloads") {
    def requestId = column[UUID]("update_request_id")
    def vin = column[Vehicle.IdentificationNumber]("vin")
    def downloadIndex = column[Int]("download_index")
    def packageIndex = column[Int]("package_index")
    def packageName = column[Package.Name]("package_name")
    def packageVersion = column[Package.Version]("package_version")

    def pk = primaryKey("pk_downloads", (requestId, vin, downloadIndex, packageName, packageVersion))

    def * = (requestId, vin, downloadIndex, packageIndex, packageName, packageVersion)
  }

  val updateSpecs = TableQuery[UpdateSpecsTable]

  val downloads = TableQuery[DownloadsTable]

  def persist(updateSpec: UpdateSpec) : DBIO[Unit] = {
    val specProjection = (updateSpec.request.id, updateSpec.vin, updateSpec.chunkSize, updateSpec.status)

    def downloadProjection(downloadIndex: Int)(p: Package, packageIndex: Int) =
      (updateSpec.request.id, updateSpec.vin, downloadIndex, packageIndex, p.id.name, p.id.version)

    import cats._
    import cats.std.all._
    val F = Foldable[Vector]

    val reqDownloads : Vector[Vector[(UUID, Vehicle.IdentificationNumber, Int, Int, Package.Name, Package.Version)]] = updateSpec.downloads.zipWithIndex.map {
      case (download, index) => download.packages.zipWithIndex.map( downloadProjection(index) _ tupled )
    }

    DBIO.seq(
      updateSpecs += specProjection,
      downloads ++= F.foldK(reqDownloads)
    )
  }


}
