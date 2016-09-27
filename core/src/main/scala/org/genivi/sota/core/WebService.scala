/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Directives}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.resolver.{Connectivity, ExternalResolverClient}
import org.genivi.sota.core.transfer.UpdateNotifier
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.rest.Validation.refined

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api.Database
import Directives._
import org.genivi.sota.messaging.MessageBusPublisher

object WebService {
  val extractUuid = refined[Uuid.Valid](Slash ~ Segment)
  val extractDeviceUuid = refined[Uuid.Valid](Slash ~ Segment).map(Uuid(_))

  def allowExtractor[T](namespaceExtractor: Directive1[Namespace],
                          extractor: Directive1[T],
                          allowFn: (T => Future[Namespace])): Directive1[T] = {
    (extractor & namespaceExtractor).tflatMap { case (value, ns) =>
      onSuccess(allowFn(value)).flatMap {
        case namespace if namespace == ns =>
          provide(value)
        case _ =>
          reject(AuthorizationFailedRejection)
      }
    }
  }
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

  val campaignResource = new CampaignResource(db, authNamespace)
  val devicesResource = new DevicesResource(db, connectivity.client, resolver, deviceRegistry, authNamespace)
  val packagesResource = new PackagesResource(resolver, db, messageBusPublisher, authNamespace)
  val updateService = new UpdateService(notifier, deviceRegistry)
  val updateRequestsResource = new UpdateRequestsResource(db, resolver, updateService, authNamespace)
  val historyResource = new HistoryResource(authNamespace)(db, system)
  val blacklistResource = new BlacklistResource(authNamespace, messageBusPublisher)(db, system)
  val impactResource = new ImpactResource(authNamespace, resolver)(db, system)

  val route = (handleErrors & pathPrefix("api" / "v1")) {
    campaignResource.route ~
    devicesResource.route ~
    packagesResource.route ~
    updateRequestsResource.route ~
    historyResource.route ~
    blacklistResource.route ~
    impactResource.route
  }
}
