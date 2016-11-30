/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.data._
import org.genivi.sota.device_registry.db._
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

class DevicePackagesResource(namespaceExtractor: Directive1[AuthedNamespaceScope],
                             deviceNamespaceAuthorizer: Directive1[Uuid])
                            (implicit system: ActorSystem,
             db: Database,
             mat: ActorMaterializer,
             ec: ExecutionContext) {

  import Directives._

  def listPackagesOnDevice(device: Uuid): Route =
    complete(db.run(InstalledPackages.installedOn(device)))

  def updateInstalledSoftware(device: Uuid): Route = {
    entity(as[Seq[PackageId]]) { installedSoftware =>
      val f = db.run(InstalledPackages.setInstalled(device, installedSoftware.toSet))
      onSuccess(f) { complete(StatusCodes.NoContent) }
    }
  }

  def route: Route = namespaceExtractor { ns =>
    val scope = Scopes.devices(ns)
    pathPrefix("devices") {
      deviceNamespaceAuthorizer { uuid =>
        path("packages") {
          (put & ns.oauthScope(s"ota-core.{device.show}.write")) {
            updateInstalledSoftware(uuid)
          } ~
          scope.get {
            listPackagesOnDevice(uuid)
          }
        }
      }
    }
  }
}
