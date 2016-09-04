/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.client

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.marshalling.CirceMarshallingSupport

import scala.concurrent.{ExecutionContext, Future}


class DeviceRegistryClient(baseUri: Uri, devicesUri: Uri)
                          (implicit system: ActorSystem, mat: ActorMaterializer)
    extends DeviceRegistry {

  import CirceMarshallingSupport._
  import Device._
  import HttpMethods._
  import StatusCodes._

  private val log = Logging(system, "org.genivi.sota.deviceRegistryClient")

  private val http = Http()

  override def searchDevice(ns: Namespace, re: String Refined Regex)
                           (implicit ec: ExecutionContext): Future[Seq[Device]] =
    execHttp[Seq[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("regex" -> re.get, "namespace" -> ns.get))))
      .recover { case t =>
        log.error(t, "Could not contact device registry")
        Seq.empty[Device]
      }

  override def createDevice(device: DeviceT)
                           (implicit ec: ExecutionContext): Future[Id] = {
    for {
      entity <- Marshal(device).to[MessageEntity]
      req = HttpRequest(method = POST, uri = baseUri.withPath(devicesUri.path), entity = entity)
      response <- execHttp[Id](req)
    } yield response
  }

  override def fetchDevice(id: Id)
                          (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Device](HttpRequest(uri = baseUri.withPath(devicesUri.path / id.show)))

  override def fetchByDeviceId(ns: Namespace, id: DeviceId)
                              (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Seq[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("namespace" -> ns.get, "deviceId" -> id.show))))
      .flatMap {
        case d +: _ => FastFuture.successful(d)
        case _ => FastFuture.failed(Errors.MissingDevice)
      }

  override def updateDevice(id: Id, device: DeviceT)
                           (implicit ec: ExecutionContext): Future[Unit] =
    for {
      entity <- Marshal(device).to[MessageEntity]
      req = HttpRequest(method = PUT, uri = baseUri.withPath(devicesUri.path / id.show), entity = entity)
      response <- execHttp[Unit](req)
    } yield response

  override def deleteDevice(id: Id)
                  (implicit ec: ExecutionContext): Future[Unit] =
    execHttp[Unit](
      HttpRequest(method = DELETE, uri = baseUri.withPath(devicesUri.path / id.show))
    )

  override def updateLastSeen(id: Id, seenAt: Instant = Instant.now)
                             (implicit ec: ExecutionContext): Future[Unit] =
    execHttp[Unit](HttpRequest(method = POST, uri = baseUri.withPath(devicesUri.path / id.show / "ping")))

  override def updateSystemInfo(id: Id, json: Json)
                              (implicit ec: ExecutionContext): Future[Unit] =
    execHttp[Unit](HttpRequest(method = PUT, uri = baseUri.withPath(devicesUri.path / id.show / "system_info"),
      entity = HttpEntity(ContentTypes.`application/json`, json.noSpaces)))

  override def getSystemInfo(id: Id)
                            (implicit ec: ExecutionContext): Future[Json] =
    execHttp[Json](HttpRequest( method = GET , uri = baseUri.withPath(devicesUri.path / id.show / "system_info")))

  private def execHttp[T](httpRequest: HttpRequest)
                         (implicit unmarshaller: Unmarshaller[ResponseEntity, T],
                          ec: ExecutionContext): Future[T] = {
    http.singleRequest(httpRequest).flatMap { response =>
      response.status match {
        case Conflict => FastFuture.failed(Errors.ConflictingDeviceId)
        case NotFound => FastFuture.failed(Errors.MissingDevice)
        case other if other.isSuccess() => unmarshaller(response.entity)
        case err => FastFuture.failed(new Exception(err.toString))
      }
    }
  }
}
