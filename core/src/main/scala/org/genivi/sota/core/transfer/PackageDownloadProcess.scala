/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.data.{Package, UpdateSpec, UpdateStatus}
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.storage.PackageStorage.PackageRetrievalOp
import org.genivi.sota.data.Device
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.refined.SlickRefined._
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


class PackageDownloadProcess(db: Database, packageRetrieval: PackageRetrievalOp)
                            (implicit val system: ActorSystem, mat: ActorMaterializer) {
  import SlickExtensions._
  import org.genivi.sota.core.storage.PackageStorage
  import org.genivi.sota.core.data.UpdateRequest

  /**
    * <ul>
    * <li>Retrieves from [[PackageStorage]] the binary file for the package denoted by the argument.</li>
    * <li>Rewrites in the DB to [[UpdateStatus.InFlight]] the status of an [[UpdateSpec]],
    * ie for a ([[UpdateRequest]], device) combination.</li>
    * </ul>
    */
  def buildClientDownloadResponse(device: Device.Id, updateRequestId: Refined[String, Uuid])
                                 (implicit ec: ExecutionContext): Future[HttpResponse] = {
    val availablePackageIO = findForDownload(updateRequestId)

    db.run(availablePackageIO) flatMap {
      case Some(packageModel) =>
        UpdateSpecs.setStatus(device, java.util.UUID.fromString(updateRequestId.get), UpdateStatus.InFlight)
        packageRetrieval(packageModel)
      case None =>
        Future.successful(HttpResponse(StatusCodes.NotFound, entity = "Package not found"))
    }
  }

  /**
    * Each [[UpdateRequest]] refers to a single package,
    * that this method returns after database lookup.
    */
  private def findForDownload(updateRequestId: Refined[String, Uuid]): DBIO[Option[Package]] = {
    updateRequests
      .filter(_.id === updateRequestId)
      .join(Packages.packages)
      .on((updateRequest, packageM) =>
        packageM.name === updateRequest.packageName && packageM.version === updateRequest.packageVersion)
      .map { case (_, packageM) => packageM }
      .result.headOption
  }

}
