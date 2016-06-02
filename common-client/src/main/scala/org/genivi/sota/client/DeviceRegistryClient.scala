/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.client

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, MessageEntity, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.Show
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Device, DeviceT}
import org.genivi.sota.data.Namespace.Namespace
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

  private[this] val log = Logging(system, "org.genivi.sota.deviceRegistryClient")

  override def searchDevice(ns: Namespace, re: String Refined Regex)
                           (implicit ec: ExecutionContext): Future[Seq[Device]] =
    Http().singleRequest(HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("regex" -> re.get, "namespace" -> ns.get))))
      .flatMap { response: HttpResponse =>
        Unmarshal(response.entity).to[Seq[Device]]
      }.recover { case _ => Seq.empty[Device] }

  override def createDevice(device: DeviceT)
                           (implicit ec: ExecutionContext): Future[Id] =
    Marshal(device).to[MessageEntity].flatMap { entity =>
      Http().singleRequest(HttpRequest(method = POST,
                                       uri = baseUri.withPath(devicesUri.path),
                                       entity = entity))
        .flatMap { response: HttpResponse => response.status match {
          case Created => Unmarshal(response.entity).to[Id]
          case Conflict => FastFuture.failed(Errors.ConflictingDeviceId)
          case NotFound => FastFuture.failed(Errors.MissingDevice)
          case err => FastFuture.failed(new Exception(err.toString))
        }}
    }

  override def fetchDevice(id: Id)
                          (implicit ec: ExecutionContext): Future[Device] =
    Http().singleRequest(HttpRequest(uri = baseUri.withPath(devicesUri.path / implicitly[Show[Id]].show(id))))
      .flatMap { response: HttpResponse => response.status match {
        case OK => Unmarshal(response.entity).to[Device]
        case NotFound => FastFuture.failed(Errors.MissingDevice)
        case err => FastFuture.failed(new Exception(err.toString))
      }}

  override def fetchDeviceByDeviceId(ns: Namespace, id: DeviceId)
                                    (implicit ec: ExecutionContext): Future[Device] =
    Http().singleRequest(HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("namespace" -> ns.get, "deviceId" -> implicitly[Show[DeviceId]].show(id)))))
      .flatMap { response: HttpResponse => response.status match {
        case OK => Unmarshal(response.entity).to[Device]
        case NotFound => FastFuture.failed(Errors.MissingDevice)
        case err => FastFuture.failed(new Exception(err.toString))
      }}

  override def updateDevice(id: Id, device: DeviceT)
                           (implicit ec: ExecutionContext): Future[Unit] =
    Marshal(device).to[MessageEntity].flatMap { entity =>
      Http().singleRequest(HttpRequest(method = PUT,
                                       uri = baseUri.withPath(devicesUri.path / implicitly[Show[Id]].show(id)),
                                       entity = entity))
      .flatMap { response: HttpResponse => response.status match {
        case OK => FastFuture.successful(())
        case Conflict => FastFuture.failed(Errors.ConflictingDeviceId)
        case NotFound => FastFuture.failed(Errors.MissingDevice)
        case err => FastFuture.failed(new Exception(err.toString))
      }}
    }

  override def deleteDevice(id: Id)
                  (implicit ec: ExecutionContext): Future[Unit] =
    Http().singleRequest(
      HttpRequest(method = DELETE, uri = baseUri.withPath(devicesUri.path / implicitly[Show[Id]].show(id)))
    ).flatMap { response: HttpResponse => response.status match {
      case OK => FastFuture.successful(())
      case NotFound => FastFuture.failed(Errors.MissingDevice)
      case err => FastFuture.failed(new Exception(err.toString))
    }}

  override def updateLastSeen(id: Id, seenAt: Instant = Instant.now)
                             (implicit ec: ExecutionContext): Future[Unit] =
    Http().singleRequest(HttpRequest(method = POST,
                                     uri = baseUri.withPath(devicesUri.path / implicitly[Show[Id]].show(id) / "ping")))
      .flatMap { response: HttpResponse =>
        response.status match {
          case OK => FastFuture.successful(())
          case NotFound => FastFuture.failed(Errors.MissingDevice)
          case err => FastFuture.failed(new Exception(err.toString))
        }
      }

}
