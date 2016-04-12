/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.server.PathMatchers.Slash
import akka.http.scaladsl.server.Directives
import Directives._
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.string._
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import akka.http.scaladsl.marshalling.Marshaller._
import eu.timepit.refined.api.Refined
import org.genivi.sota.core.data._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database
import io.circe.syntax._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.db.UpdateSpecs

class UpdateRequestsResource(db: Database, resolver: ExternalResolverClient, updateService: UpdateService)
                            (implicit system: ActorSystem, mat: ActorMaterializer) {

  import UpdateSpec._
  import CirceMarshallingSupport._
  import system.dispatcher
  import eu.timepit.refined.string.uuidValidate
  import WebService._

  implicit val _db = db

  def fetch(uuid: Refined[String, Uuid]) = {
    complete(db.run(UpdateSpecs.listUpdatesById(uuid)))
  }

  def queueVehicleUpdate(vin: Vehicle.Vin) = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueVehicleUpdate(vin, packageId)
      complete(result)
    }
  }

  def fetchUpdates = {
    complete(updateService.all(db, system.dispatcher))
  }

  def createUpdate = {
    entity(as[UpdateRequest]) { req =>
      complete(
        updateService.queueUpdate(
          req,
          pkg => resolver.resolve(pkg.id).map {
            m => m.map { case (v, p) => (v.vin, p) }
          }
        )
      )
    }
  }

  val route = pathPrefix("updates") {
    (get & extractUuid & pathEnd) {
      fetch
    } ~
      (extractVin & post) { queueVehicleUpdate } ~
      pathEnd {
        get {
          fetchUpdates
        } ~
        post {
          createUpdate
        }
    }
  }
}
