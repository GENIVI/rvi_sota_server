/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import java.time.Instant

import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.http.ErrorHandler._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import org.genivi.sota.core.db.{BlacklistedPackageRequest, BlacklistedPackages, UpdateSpecs}
import org.genivi.sota.data.{Namespace, PackageId, UpdateStatus}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.PackageBlacklisted
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.core.transfer.DeviceUpdates
import org.genivi.sota.rest.ToResponse

case class PreviewResponse(affected_device_count: Int)

class BlacklistResource(namespaceExtractor: Directive1[AuthedNamespaceScope],
                        messageBus: MessageBusPublisher)
                       (implicit db: Database, system: ActorSystem) {

  import akka.http.scaladsl.server.Directives._
  import PackagesResource.extractPackageId
  import org.genivi.sota.rest.ResponseConversions._
  import org.genivi.sota.core.db.BlacklistedPackageResponse._

  implicit val _ec = system.dispatcher

  def addPackageToBlacklist(namespace: Namespace): Route =
    entity(as[BlacklistedPackageRequest]) { req =>
      val f = for {
        bl <- BlacklistedPackages.create(namespace, req.packageId, req.comment)
        _ <- messageBus.publishSafe(PackageBlacklisted(namespace, req.packageId, Instant.now()))
        _ <- db.run(UpdateSpecs.cancelAllUpdatesByStatus(UpdateStatus.Pending, namespace, req.packageId))
        _ <- db.run(UpdateSpecs.findByPackageId(namespace, req.packageId)).map { res =>
          res.map {
            case (usr, packageUuid) => messageBus.publish(UpdateSpec(namespace, usr.device, packageUuid,
              UpdateStatus.Canceled))
          }
        }
      } yield StatusCodes.Created

      complete(f)
    }

  def updatePackageBlacklist(namespace: Namespace): Route =
    entity(as[BlacklistedPackageRequest]) { req =>
      complete(BlacklistedPackages.update(namespace, req.packageId, req.comment).map(_ => StatusCodes.OK))
    }

  def getNamespaceBlacklist(namespace: Namespace): Route =
    complete(BlacklistedPackages.findFor(namespace).map(_.toResponse))

  def getPackageBlacklist(namespace: Namespace, packageId: PackageId): Route = {
    complete(BlacklistedPackages.findByPackageId(namespace, packageId).map(ToResponse(_)))
  }

  def deletePackageBlacklist(namespace: Namespace, packageId: PackageId): Route =
    complete(BlacklistedPackages.remove(namespace, packageId).map(_ => StatusCodes.OK))

  def preview(ns: Namespace, pkgId: PackageId): Route =
    complete(db.run(DeviceUpdates.countQueuedBy(pkgId, ns)).map(PreviewResponse))

  val route: Route =
    (handleErrors & pathPrefix("blacklist") & namespaceExtractor) { ns =>
      val scope = Scopes.packages(ns)
      pathEnd {
        scope.post { addPackageToBlacklist(ns) } ~
        scope.put { updatePackageBlacklist(ns) } ~
        scope.get { getNamespaceBlacklist(ns) }
      } ~
      extractPackageId { pkgId =>
        (path("preview") & scope.get) { preview(ns, pkgId) } ~
        pathEnd {
          scope.get { getPackageBlacklist(ns, pkgId) } ~
          scope.delete { deletePackageBlacklist(ns, pkgId) }
        }
      }
    }
}
