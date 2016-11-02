/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.packages

import akka.actor.ActorSystem
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.Namespace
import org.genivi.sota.http.ErrorHandler
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

class PackageFiltersResource(namespaceDirective: Directive1[Namespace], deviceRegistry: DeviceRegistry)
                            (implicit system: ActorSystem,
                             db: Database, mat:
                             ActorMaterializer,
                             ec: ExecutionContext) extends Directives {

  import org.genivi.sota.resolver.common.RefinementDirectives._

  val packageDirectives = new PackageDirectives(namespaceDirective, deviceRegistry)

  val routes: Route = ErrorHandler.handleErrors {
    (pathPrefix("package_filters") & refinedPackageId & namespaceDirective) { (pid, ns) =>
      packageDirectives.packageFilterApi(ns, pid)
    }
  }
}
