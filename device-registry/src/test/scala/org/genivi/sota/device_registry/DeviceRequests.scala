/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import akka.http.scaladsl.client.RequestBuilding.{Delete, Get, Post, Put}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, Uri}
import cats.Show
import io.circe.generic.auto._
import org.genivi.sota.datatype.Namespace
import org.genivi.sota.data.{Device, DeviceT}
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import scala.concurrent.ExecutionContext


/**
 * Generic test resource object
 * Used in property-based testing
 */
object Resource {
  def uri(pathSuffixes: String*): Uri = {
    val BasePath = Path("/api") / "v1"
    Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))
  }
}

/**
 * Testing Trait for building Device requests
 */
trait DeviceRequests {

  import Device._

  val api = "devices"

  def fetchDevice(id: Id)
                 (implicit s: Show[Id]): HttpRequest =
    Get(Resource.uri(api, s.show(id)).withQuery(Query("namespace" -> "default")))

  def searchDevice(namespace: Namespace, regex: String): HttpRequest =
    Get(Resource.uri(api).withQuery(Query("namespace" -> namespace.get, "regex" -> regex)))

  def fetchDeviceByDeviceId(namespace: Namespace, deviceId: Device.DeviceId)
                           (implicit s: Show[Device.DeviceId]): HttpRequest =
    Get(Resource.uri(api).withQuery(Query("namespace" -> namespace.get, "deviceId" -> s.show(deviceId))))

  def updateDevice(id: Id, device: DeviceT)
                  (implicit s: Show[Id], ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(api, s.show(id)), device)

  def createDevice(device: DeviceT)
                  (implicit ec: ExecutionContext): HttpRequest = {
    Post(Resource.uri(api), device)
  }

  def deleteDevice(id: Id)
                  (implicit s: Show[Id]): HttpRequest =
    Delete(Resource.uri(api, s.show(id)))

  def updateLastSeen(id: Id)
                    (implicit s: Show[Id]): HttpRequest =
    Post(Resource.uri(api, s.show(id), "ping"))

}
