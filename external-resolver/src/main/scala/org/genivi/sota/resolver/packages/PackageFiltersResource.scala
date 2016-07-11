/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.packages

import akka.actor.ActorSystem
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import org.genivi.sota.data.Namespace.Namespace
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

class PackageFiltersResource(namespaceDirective: Directive1[Namespace])
                            (implicit system: ActorSystem,
                             db: Database, mat:
                             ActorMaterializer,
                             ec: ExecutionContext) extends Directives {

  import org.genivi.sota.resolver.common.RefinementDirectives._

  val packageDirectives = new PackageDirectives(namespaceDirective)

  val routes: Route = (pathPrefix("package_filters") & refinedPackageId & namespaceDirective) { (pid, ns) =>
    packageDirectives.packageFilterApi(ns, pid)
  }
}
