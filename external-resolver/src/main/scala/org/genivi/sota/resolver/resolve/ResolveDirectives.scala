/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives._

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import Directives._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.db.DbDepResolver


/**
 * API routes for package resolution.
 */
class ResolveDirectives(namespaceExtractor: Directive1[Namespace],
                        deviceRegistry: DeviceRegistry)
                       (implicit system: ActorSystem,
                        db: Database,
                        mat: ActorMaterializer,
                        ec: ExecutionContext) {

  def resolvePackage(ns: Namespace, id: PackageId): Route = {
    val resultF = DbDepResolver.resolve(ns, deviceRegistry, id)

    complete(resultF)
  }

  implicit val NamespaceUnmarshaller: FromStringUnmarshaller[Namespace] = Unmarshaller.strict(Namespace.apply)

  def route: Route = ErrorHandler.handleErrors {
    (get &
      encodeResponse &
      pathPrefix("resolve") &
      parameter('namespace.as[Namespace]) & refinedPackageIdParams) { (ns, id) =>
      resolvePackage(ns, id)
    }
  }
}
