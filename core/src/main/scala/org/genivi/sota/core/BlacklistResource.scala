/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import org.genivi.sota.http.ErrorHandler._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import org.genivi.sota.core.db.{BlacklistedPackageRequest, BlacklistedPackages}
import org.genivi.sota.data.{Namespace, PackageId}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import io.circe.generic.auto._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.PackageBlacklisted
import org.genivi.sota.messaging.Messages._
import MessageBusPublisher._

class BlacklistResource(namespaceExtractor: Directive1[Namespace],
                        messageBus: MessageBusPublisher)
                       (implicit db: Database, system: ActorSystem) {

  import akka.http.scaladsl.server.Directives._
  import PackagesResource.extractPackageId

  implicit val _ec = system.dispatcher

  def addPackageToBlacklist(namespace: Namespace): Route =
    entity(as[BlacklistedPackageRequest]) { req =>
      val f =
        BlacklistedPackages
          .create(namespace, req.packageId, req.comment)
          .pipeToBus(messageBus)(_ => PackageBlacklisted(namespace, req.packageId))
          .map(_ => StatusCodes.Created)

      complete(f)
    }

  def updatePackageBlacklist(namespace: Namespace): Route =
    entity(as[BlacklistedPackageRequest]) { req =>
      complete(BlacklistedPackages.update(namespace, req.packageId, req.comment).map(_ => StatusCodes.OK))
    }

  def getNamespaceBlacklist(namespace: Namespace): Route =
    complete(BlacklistedPackages.findFor(namespace))

  def getPackageBlacklist(namespace: Namespace, packageId: PackageId): Route =
    complete(BlacklistedPackages.findFor(namespace))

  def deletePackageBlacklist(namespace: Namespace, packageId: PackageId): Route =
    complete(BlacklistedPackages.remove(namespace, packageId).map(_ => StatusCodes.OK))

  val route: Route =
    (handleErrors & pathPrefix("blacklist") & namespaceExtractor) { ns =>
      post { addPackageToBlacklist(ns) } ~
      put { updatePackageBlacklist(ns) } ~
      get { getNamespaceBlacklist(ns) } ~
      extractPackageId { pkgId =>
        get { getPackageBlacklist(ns, pkgId) } ~
        delete { deletePackageBlacklist(ns, pkgId) }
      }
    }
}
