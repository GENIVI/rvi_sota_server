/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.core.data._
import org.genivi.sota.core.data.client._
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.data.Device
import org.genivi.sota.data.Namespace._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database

class UpdateRequestsResource(db: Database, resolver: ExternalResolverClient, updateService: UpdateService,
                             namespaceExtractor: Directive1[Namespace])
                            (implicit system: ActorSystem, mat: ActorMaterializer) {

  import CirceMarshallingSupport._
  import Directives._
  import UpdateSpec._
  import WebService._
  import eu.timepit.refined.string.uuidValidate
  import system.dispatcher
  import ClientUpdateRequest._
  import RequestConversions._

  implicit val _db = db

  /**
    * An ota client GET (device, status) for the [[UpdateRequest]] given by the argument.
    */
  def fetch(updateRequestId: Refined[String, Uuid]): Route = {
    complete(db.run(UpdateSpecs.listUpdatesById(updateRequestId)))
  }

  /**
    * An ota client GET all rows in the [[UpdateRequest]] table.
    */
  def fetchUpdates: Route = {
    complete(updateService.all(db, system.dispatcher))
  }

  private def clientUpdateRequest(ns: Namespace): Directive1[UpdateRequest] = {
    clientEntity(ns)
  }

  /**
    * An ota client POST an [[UpdateRequest]] campaign to locally persist it along with one or more [[UpdateSpec]]
    * (one per device, for dependencies obtained from resolver) thus scheduling an update.
    */
  def createUpdate(ns: Namespace): Route = {
    clientUpdateRequest(ns) { req: UpdateRequest =>
      complete(
        updateService.queueUpdate(
          req,
          pkg => resolver.resolve(ns, pkg.id).map {
            m => m.map { case (v, p) =>
              // deviceRegistry.fetchDeviceByDeviceId(Device.DeviceId(v.vin.get))
              (v.vin, p)
            }
          }
        ) map (_ => (StatusCodes.Created, req))
      )
    }
  }

  val route = pathPrefix("update_requests") {
    (get & extractUuid & pathEnd) {
      fetch
    } ~
    pathEnd {
      get {
        fetchUpdates
      } ~
      (post & namespaceExtractor) { ns =>
        createUpdate(ns)
      }
    }
  }
}
