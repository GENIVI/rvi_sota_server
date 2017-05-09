/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.{BlacklistedPackages, Packages, UpdateSpecs}
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.storage.PackageStorage.PackageRetrievalOp
import org.genivi.sota.data.{UpdateStatus, Uuid}
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.refined.SlickRefined._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}


class PackageDownloadProcess(db: Database, packageRetrieval: PackageRetrievalOp)
                            (implicit val system: ActorSystem, mat: ActorMaterializer) {
  import SlickExtensions._

  /**
    * <ul>
    * <li>Retrieves from [[PackageStorage]] the binary file for the package denoted by the argument.</li>
    * <li>Rewrites in the DB to [[UpdateStatus.InFlight]] the status of an [[UpdateSpec]],
    * ie for a ([[UpdateRequest]], device) combination.</li>
    * </ul>
    */
  def buildClientDownloadResponse(device: Uuid, updateRequestId: Refined[String, Uuid.Valid])
                                 (implicit ec: ExecutionContext): Future[(Package, HttpResponse)] = {
    val dbIO = for {
      pkg <- findForDownload(updateRequestId)
      _ <- UpdateSpecs.setStatus(device, UUID.fromString(updateRequestId.get), UpdateStatus.InFlight)
    } yield pkg

    for {
      pkg <- db.run(dbIO.transactionally)
      resp <- packageRetrieval(pkg)
    } yield (pkg, resp)
  }

  /**
    * Each [[UpdateRequest]] refers to a single package,
    * that this method returns after database lookup.
    */
  private def findForDownload(updateRequestId: Refined[String, Uuid.Valid])
                             (implicit ec: ExecutionContext): DBIO[Package] = {
    updateRequests
      .filter(_.id === updateRequestId)
      .join(Packages.packages).on(_.packageUuid === _.uuid)
      .map { case (_, packageM) => packageM }
      .result
      .failIfNotSingle(SotaCoreErrors.MissingPackage)
      .flatMap(BlacklistedPackages.ensureNotBlacklisted)
  }
}
