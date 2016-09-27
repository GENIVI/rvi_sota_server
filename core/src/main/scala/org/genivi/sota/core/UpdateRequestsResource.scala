/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.core.data._
import org.genivi.sota.core.data.client._
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs}
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.Future

class UpdateRequestsResource(db: Database, resolver: ExternalResolverClient, updateService: UpdateService,
                             namespaceExtractor: Directive1[Namespace])
                            (implicit system: ActorSystem, mat: ActorMaterializer) {

  import CirceMarshallingSupport._
  import Directives._
  import UpdateSpec._
  import WebService._
  import system.dispatcher
  import ClientUpdateRequest._
  import org.genivi.sota.rest.RequestConversions._

  implicit val _db = db

  /**
    * An ota client GET (device, status) for the [[UpdateRequest]] given by the argument.
    */
  def fetch(updateRequestId: Uuid): Route = {
    complete(db.run(UpdateSpecs.listUpdatesById(updateRequestId)))
  }

  /**
    * An ota client GET all rows in the [[UpdateRequest]] table.
    */
  def fetchUpdates(namespace: Namespace): Route = {
    complete(updateService.all(namespace))
  }

  private def clientUpdateRequest(ns: Namespace): Directive[(ClientUpdateRequest, UpdateRequest)] = {
    import shapeless.HNil

    val f = (entity: ClientUpdateRequest) => db.run(Packages.byId(ns, entity.packageId)).map { p =>
      p.uuid :: p.namespace :: HNil
    }

    fromRequest(f)
  }

  /**
    * An ota client POST an [[UpdateRequest]] campaign to locally persist it along with one or more [[UpdateSpec]]
    * (one per device, for dependencies obtained from resolver) thus scheduling an update.
    */
  def createUpdate(ns: Namespace): Route = {
    import ClientUpdateRequest._
    import org.genivi.sota.rest.ResponseConversions._

    clientUpdateRequest(ns) { case (creq: ClientUpdateRequest, req: UpdateRequest) =>
      val resultF = updateService.queueUpdate(ns, req, pkg => resolver.resolve(ns, pkg.id))
      complete(resultF.map (_ => (StatusCodes.Created, req.toResponse(creq.packageId))))
    }
  }

  def updateRequestAllowed(uuid: Uuid): Future[Namespace] = {
    db.run(UpdateRequests.byId(uuid.toJava).map(_.namespace))
  }

  val uuidExtractor = allowExtractor(namespaceExtractor, extractUuid.map(Uuid(_)), updateRequestAllowed)

  val route = pathPrefix("update_requests") {
    (get & uuidExtractor & pathEnd) {
      fetch
    } ~
      (namespaceExtractor & pathEnd) { ns =>
        get {
          fetchUpdates(ns)
        } ~
        post {
          createUpdate(ns)
        }
      }
  }
}
