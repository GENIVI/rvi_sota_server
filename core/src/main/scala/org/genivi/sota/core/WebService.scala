/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.stream.ActorMaterializer
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.resolver.{Connectivity, ExternalResolverClient}
import org.genivi.sota.http.AuthedNamespaceScope

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api.Database
import org.genivi.sota.messaging.MessageBusPublisher

class WebService(updateService: UpdateService,
                 resolver: ExternalResolverClient,
                 deviceRegistry: DeviceRegistry,
                 db: Database,
                 authNamespace: Directive1[AuthedNamespaceScope],
                 messageBusPublisher: MessageBusPublisher)
                (implicit val system: ActorSystem, val mat: ActorMaterializer,
                 val connectivity: Connectivity, val ec: ExecutionContext) extends Directives {
  implicit val log = Logging(system, "webservice")

  import org.genivi.sota.http.ErrorHandler._

  val devicesResource = new DevicesResource(db, connectivity.client, resolver, deviceRegistry, authNamespace)
  val packagesResource = new PackagesResource(updateService, db, messageBusPublisher, authNamespace)
  val autoInstallResource = new AutoInstallResource(db, deviceRegistry, authNamespace)
  val updateRequestsResource = new UpdateRequestsResource(db, resolver, updateService, authNamespace,
                                                          messageBusPublisher)
  val historyResource = new HistoryResource(deviceRegistry, authNamespace)(db, system)
  val blacklistResource = new BlacklistResource(authNamespace, messageBusPublisher)(db, system)
  val impactResource = new ImpactResource(authNamespace, deviceRegistry)(db, system)
  val campaignResource =
    new CampaignResource(authNamespace, deviceRegistry, updateService, messageBusPublisher)(db, system)

  val route = (handleErrors & pathPrefix("api" / "v1")) {
    campaignResource.route ~
    devicesResource.route ~
    packagesResource.route ~
    autoInstallResource.route ~
    updateRequestsResource.route ~
    historyResource.route ~
    blacklistResource.route ~
    impactResource.route
  }
}
