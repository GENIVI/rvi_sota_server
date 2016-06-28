/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.string.Uuid
import eu.timepit.refined.string._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.resolver.{Connectivity, ExternalResolverClient}
import org.genivi.sota.core.transfer.UpdateNotifier
import org.genivi.sota.data.Device
import org.genivi.sota.data.Namespace._
import org.genivi.sota.datatype.NamespaceDirective
import org.genivi.sota.rest.Validation.refined
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api.Database

import Directives._


object WebService {
  // val extractVin : Directive1[Device.Id] = refined[Device.ValidVin](Slash ~ Segment)
  val extractDeviceUuid : Directive1[Device.Id] = refined[Uuid](Slash ~ Segment).map(Device.Id(_))
  val extractUuid = refined[Uuid](Slash ~ Segment)
}

class WebService(notifier: UpdateNotifier,
                 resolver: ExternalResolverClient,
                 deviceRegistry: DeviceRegistry,
                 db: Database)
                (implicit val system: ActorSystem, val mat: ActorMaterializer,
                 val connectivity: Connectivity, val ec: ExecutionContext) extends Directives {
  implicit val log = Logging(system, "webservice")

  import ErrorHandler._
  import NamespaceDirective._

  val vehicles = new DevicesResource(db, connectivity.client, resolver, deviceRegistry, defaultNamespaceExtractor)
  val packages = new PackagesResource(resolver, db, defaultNamespaceExtractor)
  val updateService = new UpdateService(notifier, deviceRegistry)
  val updateRequests = new UpdateRequestsResource(db, resolver, updateService, defaultNamespaceExtractor)
  val history = new HistoryResource(db, defaultNamespaceExtractor)

  val route = (handleErrors & pathPrefix("api" / "v1")) {
    vehicles.route ~ packages.route ~ updateRequests.route ~ history.route
  }
}
