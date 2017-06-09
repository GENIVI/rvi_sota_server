/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import java.time.Instant

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import java.util.Base64

import org.genivi.sota.data.{DeviceT, Namespace, Uuid}
import org.genivi.sota.device_registry.db.{DeviceRepository, PublicCredentialsRepository}
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeviceCreated, DevicePublicCredentialsSet}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object PublicCredentialsResource {
  final case class FetchPublicCredentials(uuid: Uuid, credentials: String)
}

class PublicCredentialsResource(authNamespace: Directive1[AuthedNamespaceScope],
                          messageBus: MessageBusPublisher,
                          deviceNamespaceAuthorizer: Directive1[Uuid])
                         (implicit db: Database,
                          mat: ActorMaterializer,
                          ec: ExecutionContext) {
  import PublicCredentialsResource._
  lazy val base64Decoder = Base64.getDecoder()
  lazy val base64Encoder = Base64.getEncoder()

  def fetchPublicCredentials(uuid: Uuid): Route =
    complete(db.run(PublicCredentialsRepository.findByUuid(uuid)).map { creds =>
               FetchPublicCredentials(uuid, new String(creds))
             })

  def createDeviceWithPublicCredentials(ns: Namespace, devT: DeviceT): Route = {
    val act = (devT.deviceId, devT.credentials) match {
      case (Some(devId), Some(credentials)) => {
        val dbact = for {
          (created, uuid) <- DeviceRepository.findUuidFromUniqueDeviceIdOrCreate(ns, devId, devT)
          _ <- PublicCredentialsRepository.update(uuid, credentials.getBytes)
        } yield (created, uuid)

        for {
          (created, uuid) <- db.run(dbact.transactionally)
          _ <- if (created) {
            messageBus.publish(DeviceCreated(ns, uuid, devT.deviceName, devT.deviceId, devT.deviceType, Instant.now()))
          } else {Future.successful(())}
          _ <- messageBus.publish(DevicePublicCredentialsSet(ns, uuid, credentials, Instant.now()))
        } yield uuid
      }
      case (None, _) => FastFuture.failed(Errors.RequestNeedsDeviceId)
      case (_, None) => FastFuture.failed(Errors.RequestNeedsCredentials)
    }
    complete(act)
  }

  def api: Route =
    (pathPrefix("devices") & authNamespace) { ns =>
      val scope = Scopes.devices(ns)
      pathEnd {
        (scope.put & entity(as[DeviceT])) { devT => createDeviceWithPublicCredentials(ns, devT) }
      } ~
      deviceNamespaceAuthorizer { uuid =>
        path("public_credentials") {
          scope.get {
            fetchPublicCredentials(uuid)
          }
        }
      }
    }

  val route: Route = api
}
