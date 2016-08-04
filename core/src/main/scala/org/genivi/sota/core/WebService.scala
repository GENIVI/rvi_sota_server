/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.string.Uuid
import eu.timepit.refined.string._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.resolver.{Connectivity, ExternalResolverClient}
import org.genivi.sota.core.transfer.UpdateNotifier
import org.genivi.sota.data.Device
import org.genivi.sota.data.Namespace
import org.genivi.sota.rest.Validation.refined

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api.Database
import Directives._
import cats.data.Xor
import eu.timepit.refined.api.Refined
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.PackageCreated

object WebService {
  val extractDeviceUuid : Directive1[Device.Id] = refined[Uuid](Slash ~ Segment).map(Device.Id)
  val extractUuid = refined[Uuid](Slash ~ Segment)
}

class WebService(notifier: UpdateNotifier,
                 resolver: ExternalResolverClient,
                 deviceRegistry: DeviceRegistry,
                 db: Database,
                 authNamespace: Directive1[Namespace],
                 messageBusPublisher: MessageBusPublisher)
                (implicit val system: ActorSystem, val mat: ActorMaterializer,
                 val connectivity: Connectivity, val ec: ExecutionContext) extends Directives {
  implicit val log = Logging(system, "webservice")

  import ErrorHandler._
  import ErrorHandler._
  import PackagesResource._
  import WebService._
  import eu.timepit.refined._
  import org.genivi.sota.marshalling.RefinedMarshallingSupport._

  val devicesResource = new DevicesResource(db, connectivity.client, resolver, deviceRegistry, authNamespace)
  val packagesResource = new PackagesResource(resolver, db, messageBusPublisher, authNamespace)
  val updateService = new UpdateService(notifier, deviceRegistry)
  val updateRequestsResource = new UpdateRequestsResource(db, resolver, updateService, authNamespace)
  val historyResource = new HistoryResource(db, authNamespace)

  val deviceRoutes: Route = pathPrefix("devices") {
    extractExecutionContext { implicit ec =>
      authNamespace { ns =>
        (pathEnd & get) { devicesResource.search(ns) }
      }
    }
  }

  val packageRoutes: Route =
    pathPrefix("packages") {
      authNamespace { ns =>
        (pathEnd & get) { packagesResource.searchPackage(ns) } ~
          extractPackageId { pid =>
            pathEnd {
              get { packagesResource.fetch(ns, pid) } ~
                put { packagesResource.updatePackage(ns, pid) }
            } ~
              path("queued") { packagesResource.queuedDevices(ns, pid) }
          }
      }
    }


  val updateRequestRoute: Route =
    pathPrefix("update_requests") {
      (get & extractUuid) { updateRequestsResource.fetch } ~
        pathEnd {
          get { updateRequestsResource.fetchUpdates } ~
            (post & authNamespace) { updateRequestsResource.createUpdate }
        }
    }

  val historyRoutes: Route = {
    (pathPrefix("history") & parameter('uuid.as[String Refined Device.ValidId])) { uuid =>
      authNamespace { ns =>
        (get & pathEnd) {
          historyResource.history(ns, Device.Id(uuid))
        }
      }
    }
  }

  val route = (handleErrors & pathPrefix("api" / "v1")) {
    deviceRoutes ~ packageRoutes ~ updateRequestRoute ~ historyRoutes
  }
}
