/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */

package org.genivi.sota.data

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Directives}
import Directives._
import eu.timepit.refined.api.Refined
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.device_registry.common.{Errors => DeviceRegistryErrors}
import akka.http.scaladsl.marshalling.Marshaller._
import org.genivi.sota.http.UuidDirectives.extractUuid
import org.genivi.sota.marshalling.RefinedMarshallingSupport._

import scala.concurrent.ExecutionContext

trait DeviceDirectives {
  val deviceRegistry: DeviceRegistry

  def deviceAllowed(uuid: Uuid, ns: AuthedNamespaceScope)(implicit ec: ExecutionContext): Directive1[Uuid] = {
    import scala.util.{Success, Failure}
    val f = deviceRegistry.fetchDevice(ns, uuid)

    onComplete(f) flatMap {
      case Success(device) => provide(uuid)
      case Failure(DeviceRegistryErrors.MissingDevice) => complete(DeviceRegistryErrors.MissingDevice)
      case Failure(t) => reject(AuthorizationFailedRejection)
    }
  }

  def deviceQueryExtractor(paramName: Symbol, ns: AuthedNamespaceScope): Directive1[Uuid] =
    parameter(paramName.as[Refined[String, Uuid.Valid]]).flatMap { uuid =>
      extractExecutionContext.flatMap { implicit ec =>
        deviceAllowed(Uuid(uuid), ns)
      }
    }

  def devicePathExtractor(ns: AuthedNamespaceScope): Directive1[Uuid] =
    extractExecutionContext.flatMap { implicit ec =>
      extractUuid.flatMap { uuid =>
        deviceAllowed(uuid, ns)
      }
    }

}

