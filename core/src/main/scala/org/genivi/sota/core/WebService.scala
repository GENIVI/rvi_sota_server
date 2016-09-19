/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
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

  import org.genivi.sota.http.ErrorHandler._
  import PackagesResource._
  import WebService._
  import eu.timepit.refined._
  import org.genivi.sota.marshalling.RefinedMarshallingSupport._

  val devicesResource = new DevicesResource(db, connectivity.client, resolver, deviceRegistry, authNamespace)
  val packagesResource = new PackagesResource(resolver, db, messageBusPublisher, authNamespace)
  val updateService = new UpdateService(notifier, deviceRegistry)
  val updateRequestsResource = new UpdateRequestsResource(db, resolver, updateService, authNamespace)
  val historyResource = new HistoryResource(db)
  val blacklistResource = new BlacklistResource(authNamespace, messageBusPublisher)(db, system)
  val impactResource = new ImpactResource(authNamespace, resolver)(db, system)

  val route = (handleErrors & pathPrefix("api" / "v1")) {
    devicesResource.route ~
      packagesResource.route ~
      updateRequestsResource.route ~
      historyResource.route ~
      blacklistResource.route ~
      impactResource.route
  }
}
