/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.core.data._
import org.genivi.sota.core.data.client._
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.data.{Device, Namespace, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.rest.Validation._
import shapeless.HNil
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
  def fetch(updateRequestId: Refined[String, Uuid.Valid]): Route = {
    complete(db.run(UpdateSpecs.listUpdatesById(updateRequestId)))
  }

  /**
    * An ota client GET all rows in the [[UpdateRequest]] table.
    */
  def fetchUpdates: Route = {
    complete(updateService.all(db, system.dispatcher))
  }

  private def clientUpdateRequest(ns: Namespace): Directive[(ClientUpdateRequest, UpdateRequest)] = {
    import ClientUpdateRequest._
    import shapeless.HNil

    val f = (entity: ClientUpdateRequest) => db.run(Packages.byId(ns, entity.packageId)).map { p =>
      p.uuid :: HNil
    }

    fromRequest(f)
  }

  /**
    * An ota client POST an [[UpdateRequest]] campaign to locally persist it along with one or more [[UpdateSpec]]
    * (one per device, for dependencies obtained from resolver) thus scheduling an update.
    */
  def createUpdate(ns: Namespace): Route = {
    import ClientUpdateRequest._
    import ResponseConversions._

    clientUpdateRequest(ns) { case (creq: ClientUpdateRequest, req: UpdateRequest) =>
      val resultF = updateService.queueUpdate(ns, req, pkg => resolver.resolve(ns, pkg.id))
      complete(resultF.map (_ => (StatusCodes.Created, req.toResponse(creq.packageId))))
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
