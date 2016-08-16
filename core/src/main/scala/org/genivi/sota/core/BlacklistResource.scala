/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.core

import ErrorHandler._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import org.genivi.sota.core.db.{BlacklistedPackageRequest, BlacklistedPackages}
import org.genivi.sota.data.{Namespace, PackageId}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import io.circe.generic.auto._

class BlacklistResource(namespaceExtractor: Directive1[Namespace])
                       (implicit db: Database, system: ActorSystem) {

  import akka.http.scaladsl.server.Directives._
  import PackagesResource.extractPackageId

  implicit val _ec = system.dispatcher

  def addPackageToBlacklist(namespace: Namespace, packageId: PackageId): Route =
    entity(as[BlacklistedPackageRequest]) { req =>
      val f = BlacklistedPackages.create(namespace, req.packageId, req.comment).map(_ => StatusCodes.Created)
      complete(f)
    }

  def updatePackageBlacklist(ns: Namespace, packageId: PackageId): Route =
    entity(as[BlacklistedPackageRequest]) { req =>
      complete(BlacklistedPackages.update(ns, req.packageId, req.comment).map(_ => StatusCodes.OK))
    }

  def deletePackageBlacklist(namespace: Namespace, packageId: PackageId): Route =
    complete(BlacklistedPackages.remove(namespace, packageId).map(_ => StatusCodes.OK))

  def getNamespaceBlacklist(namespace: Namespace): Route = {
    complete(BlacklistedPackages.findFor(namespace))
  }

  val route: Route =
    (handleErrors & pathPrefix("blacklist") & namespaceExtractor) { ns =>
      extractPackageId { pkgId =>
        post { addPackageToBlacklist(ns, pkgId) } ~
        put { updatePackageBlacklist(ns, pkgId) } ~
        delete { deletePackageBlacklist(ns, pkgId) }
      } ~
        get {
          getNamespaceBlacklist(ns)
        }
    }
}
