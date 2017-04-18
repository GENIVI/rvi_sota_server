/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.user_profile

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.show._
import io.circe.generic.auto._
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.core.SotaCoreErrors
import scala.concurrent.{ExecutionContext, Future}


case class DeviceStatus(status: String)

trait UserProfile {

  /**
    * Checks, if a device is within the allowed limit for a user.
    */
  def checkDeviceLimit
    (ns: Namespace, device: Uuid)
    (implicit ec: ExecutionContext): Future[Boolean]

}

class UserProfileClient
  (baseUri: Uri, api: Uri)
  (implicit system: ActorSystem, mat: ActorMaterializer) extends UserProfile {

  private val log = Logging(system, "org.genivi.sota.userProfileClient")

  private val http = Http()

  override def checkDeviceLimit
    (ns: Namespace, device: Uuid)
    (implicit ec: ExecutionContext): Future[Boolean] = {

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = baseUri.withPath(api.path / "users" / ns.get / "device_status" / device.show))

    http.singleRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.NotFound =>
          log.error("Device has not been activated.")
          FastFuture.failed(SotaCoreErrors.DeviceNotActivated)
        case x if x.isSuccess() =>
          Unmarshal(response.entity).to[DeviceStatus]
            .map(_.status == "ok")
        case err =>
          log.error(err.toString, "Could not contact user profile")
          FastFuture.failed(new Exception(err.toString))
      }
    }
  }

}
