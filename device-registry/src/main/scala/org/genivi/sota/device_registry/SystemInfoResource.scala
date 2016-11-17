/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Directive1, Route}
import io.circe.Json
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.device_registry.db._
import org.genivi.sota.device_registry.common.Errors.MissingSystemInfo
import org.genivi.sota.http.UuidDirectives.extractUuid
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class SystemInfoResource(authNamespace: Directive1[AuthedNamespaceScope],
                         deviceNamespaceAuthorizer: Directive1[Uuid])
                        (implicit db: Database,
                         actorSystem: ActorSystem,
                         ec: ExecutionContext) {

  val logger = LoggerFactory.getLogger(this.getClass)


  def fetchSystemInfo(uuid: Uuid): Route = {
    val comp = db.run(SystemInfoRepository.findByUuid(uuid)).recover{
      case MissingSystemInfo => Json.obj()
    }
    complete(comp)
  }

  def createSystemInfo(uuid: Uuid, data: Json): Route = {
    val f = db.run(SystemInfoRepository.create(uuid, data))
    f.onSuccess { case _ =>
      UpdateMemberships.forDevice(uuid, data)
    }
    complete(Created -> f)
  }

  def updateSystemInfo(uuid: Uuid, data: Json): Route = {
    complete(db.run(SystemInfoRepository.update(uuid, data)))
  }

  def api: Route =
    pathPrefix("devices") {
      deviceNamespaceAuthorizer { uuid =>
        (get & path("system_info")) {
          fetchSystemInfo(uuid)
        } ~
        (post & path("system_info")) {
          entity(as[Json]) { body => createSystemInfo(uuid, body) }
        } ~
        (put & path("system_info")) {
          entity(as[Json]) { body => updateSystemInfo(uuid, body) }
        }
      }
    }

  def mydeviceRoutes: Route = authNamespace { authedNs =>
    (pathPrefix("mydevice") & extractUuid) { uuid =>
      (put & path("system_info") & authedNs.oauthScope(s"ota-core.{uuid.show}.write")) {
        entity(as[Json]) { body => updateSystemInfo(uuid, body) }
      }
    }
  }

  def route: Route = api ~ mydeviceRoutes
}
