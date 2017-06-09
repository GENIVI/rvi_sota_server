/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.db.AutoInstalls
import org.genivi.sota.data.{DeviceDirectives, Namespace, PackageId, Uuid}
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.http.Scopes
import org.genivi.sota.marshalling.CirceMarshallingSupport

import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api.Database

class AutoInstallResource
  (db: Database, val deviceRegistry: DeviceRegistry, namespaceExtractor: Directive1[AuthedNamespaceScope])
  (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext)
  extends Directives with DeviceDirectives {
  import CirceMarshallingSupport._

  def listDevice(ns: Namespace, pkgName: PackageId.Name): Route = {
    complete(db.run(AutoInstalls.listDevices(ns, pkgName)))
  }

  def listPackages(ns: Namespace, device: Uuid): Route = {
    complete(db.run(AutoInstalls.listPackages(ns, device)))
  }

  def removeAll(ns: Namespace, pkgName: PackageId.Name): Route = {
    complete(db.run(AutoInstalls.removeAll(ns, pkgName)))
  }

  def addDevice(ns: Namespace, pkgName: PackageId.Name, dev: Uuid): Route = {
    complete(db.run(AutoInstalls.addDevice(ns, pkgName, dev)))
  }

  def removeDevice(ns: Namespace, pkgName: PackageId.Name, dev: Uuid): Route = {
    complete(db.run(AutoInstalls.removeDevice(ns, pkgName, dev)))
  }

  val route = ErrorHandler.handleErrors {
    (pathPrefix("auto_install") & namespaceExtractor) { ns =>
      val scope = Scopes.updates(ns)
      (pathEnd & scope.get & deviceQueryExtractor('device, ns)) { uuid =>
        listPackages(ns, uuid)
      } ~
      PackagesResource.extractPackageName { pkgName =>
        pathEnd {
          scope.get {
            listDevice(ns, pkgName)
          } ~
          scope.delete {
            removeAll(ns, pkgName)
          }
        } ~
        devicePathExtractor(ns) { devUuid =>
          scope.put {
            addDevice(ns, pkgName, devUuid)
          } ~
          scope.delete {
            removeDevice(ns, pkgName, devUuid)
          }
        }
      }
    }
  }
}
