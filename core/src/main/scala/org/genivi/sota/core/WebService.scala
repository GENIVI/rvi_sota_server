/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core


import akka.event.Logging
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.stream.ActorMaterializer
import org.genivi.sota.core.transfer.UpdateNotifier
import slick.driver.MySQLDriver.api.Database
import org.genivi.sota.core.resolver.{Connectivity, ExternalResolverClient}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.Vehicle
import eu.timepit.refined.string.Uuid
import eu.timepit.refined._
import eu.timepit.refined.string._
import org.genivi.sota.rest.Validation.refined
import akka.http.scaladsl.model.StatusCodes._
import Directives._
import akka.actor.ActorSystem
import org.genivi.sota.datatype.NamespaceDirective


object WebService {
  val extractVin : Directive1[Vehicle.Vin] = refined[Vehicle.ValidVin](Slash ~ Segment)
  val extractUuid = refined[Uuid](Slash ~ Segment)
}

class WebService(notifier: UpdateNotifier, resolver: ExternalResolverClient, db : Database)
                (implicit val system: ActorSystem, val mat: ActorMaterializer,
                 connectivity: Connectivity) extends Directives {
  implicit val log = Logging(system, "webservice")

  import ErrorHandler._
  import NamespaceDirective._

  val vehicles = new VehiclesResource(db, connectivity.client, resolver, defaultNamespaceExtractor)
  val packages = new PackagesResource(resolver, db, defaultNamespaceExtractor)
  val updateRequests = new UpdateRequestsResource(db, resolver, new UpdateService(notifier), defaultNamespaceExtractor)
  val history = new HistoryResource(db, defaultNamespaceExtractor)

  val route = (handleErrors & pathPrefix("api" / "v1")) {
    vehicles.route ~ packages.route ~ updateRequests.route ~ history.route
  }
}
