/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.client

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.generic.auto._
import java.time.Instant
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Device, DeviceT, Namespace, Uuid}
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.http.NamespaceDirectives.nsHeader
import org.genivi.sota.marshalling.CirceMarshallingSupport
import scala.concurrent.{ExecutionContext, Future}


class DeviceRegistryClient(baseUri: Uri, devicesUri: Uri, deviceGroupsUri: Uri, mydeviceUri: Uri)
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
      .withQuery(Query("regex" -> re.get)))
      .withHeaders(nsHeader(ns)))
      .recover { case t =>
        log.error(t, "Could not contact device registry")
        Seq.empty[Device]
      }

  override def createDevice(device: DeviceT)
                           (implicit ec: ExecutionContext): Future[Uuid] = {
    for {
      entity <- Marshal(device).to[MessageEntity]
      req = HttpRequest(method = POST, uri = baseUri.withPath(devicesUri.path), entity = entity)
      response <- execHttp[Uuid](req)
    } yield response
  }

  override def fetchDevice(uuid: Uuid)
                          (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Device](HttpRequest(uri = baseUri.withPath(devicesUri.path / uuid.show)))

  override def fetchMyDevice(uuid: Uuid)
                          (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Device](HttpRequest(uri = baseUri.withPath(mydeviceUri.path / uuid.show)))

  override def fetchDevicesInGroup(uuid: Uuid)
                                  (implicit ec: ExecutionContext): Future[Seq[Uuid]] =
    execHttp[Seq[Uuid]](HttpRequest(uri = baseUri.withPath(deviceGroupsUri.path / uuid.show / "devices")))


  override def fetchGroup(uuid: Uuid)
                         (implicit ec: ExecutionContext): Future[Seq[Uuid]] =
    execHttp[Seq[Uuid]](HttpRequest(uri = baseUri.withPath(deviceGroupsUri.path / uuid.show / "devices")))

  override def fetchByDeviceId(ns: Namespace, deviceId: DeviceId)
                              (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Seq[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("deviceId" -> deviceId.show)))
      .withHeaders(nsHeader(ns)))
      .flatMap {
        case d +: _ => FastFuture.successful(d)
        case _ => FastFuture.failed(Errors.MissingDevice)
      }

  override def updateDevice(uuid: Uuid, device: DeviceT)
                           (implicit ec: ExecutionContext): Future[Unit] =
    for {
      entity <- Marshal(device).to[MessageEntity]
      req = HttpRequest(method = PUT, uri = baseUri.withPath(devicesUri.path / uuid.show), entity = entity)
      response <- execHttp[Unit](req)
    } yield response

  override def deleteDevice(uuid: Uuid)
                  (implicit ec: ExecutionContext): Future[Unit] =
    execHttp[Unit](
      HttpRequest(method = DELETE, uri = baseUri.withPath(devicesUri.path / uuid.show))
    )

  override def updateLastSeen(uuid: Uuid, seenAt: Instant = Instant.now)
                             (implicit ec: ExecutionContext): Future[Unit] =
    execHttp[Unit](HttpRequest(method = POST, uri = baseUri.withPath(mydeviceUri.path / uuid.show / "ping")))

  override def updateSystemInfo(uuid: Uuid, json: Json)
                               (implicit ec: ExecutionContext): Future[Unit] =
    execHttp[Unit](HttpRequest(method = PUT, uri = baseUri.withPath(mydeviceUri.path / uuid.show / "system_info"),
      entity = HttpEntity(ContentTypes.`application/json`, json.noSpaces)))

  override def getSystemInfo(uuid: Uuid)
                            (implicit ec: ExecutionContext): Future[Json] =
    execHttp[Json](HttpRequest( method = GET , uri = baseUri.withPath(devicesUri.path / uuid.show / "system_info")))

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
