/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import java.io.File
import java.net.URI

import akka.http.scaladsl.model._
import akka.stream.io.SynchronousFileSource
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.refined.SlickRefined._
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


class PackageDownloadProcess(db: Database) {
  import SlickExtensions._

  def buildClientDownloadResponse(uuid: Refined[String, Uuid])(implicit ec: ExecutionContext): Future[HttpResponse] = {
    val availablePackageIO = findForDownload(uuid)

    db.run(availablePackageIO).map {
      case Some(packageModel) =>
        val entity = fileEntity(packageModel)
        HttpResponse(StatusCodes.OK, entity = entity)
      case None =>
        HttpResponse(StatusCodes.NotFound, entity = "Package not found")
    }
  }

  private def findPackagesWith(updateRequestId: Refined[String, Uuid]): DBIO[Seq[Package]] = {
    updateRequests
      .filter(_.id === updateRequestId)
      .join(Packages.packages)
      .on((updateRequest, packageM) =>
        packageM.name === updateRequest.packageName && packageM.version === updateRequest.packageVersion)
      .map { case (_, packageM) => packageM }
      .result
  }

  private def findForDownload(updateRequestId: Refined[String, Uuid])
                             (implicit ec: ExecutionContext): DBIO[Option[Package]] = {
    findPackagesWith(updateRequestId).map(_.headOption)
  }

  private def fileEntity(packageModel: Package): UniversalEntity = {
    val file = new File(new URI(packageModel.uri.toString()))
    val size = file.length()
    val source = SynchronousFileSource(file)
    HttpEntity(MediaTypes.`application/octet-stream`, size, source)
  }
}
