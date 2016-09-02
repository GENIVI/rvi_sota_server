/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import akka.http.scaladsl.client.RequestBuilding.{Delete, Get, Post, Put}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, Uri}
import cats.Show
import io.circe.generic.auto._
import io.circe.Json
import org.genivi.sota.data.{Device, DeviceT, Namespace}
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

  def fetchByDeviceId(namespace: Namespace, deviceId: Device.DeviceId)
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

  def fetchSystemInfo(id: Id)
                     (implicit s: Show[Id]): HttpRequest =
    Get(Resource.uri(api, s.show(id), "system_info"))

  def createSystemInfo(id: Id, json: Json)
                      (implicit s: Show[Id], ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(api, s.show(id),"system_info"), json)

  def updateSystemInfo(id: Id, json: Json)
                      (implicit s: Show[Id], ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(api, s.show(id),"system_info"), json)

  def fetchGroupInfo(groupName: String, namespace: Namespace): HttpRequest =
    Get(Resource.uri(api, "group_info").withQuery(Query("namespace" -> namespace.get, "groupName" -> groupName)))

  def createGroupInfo(groupName: String, namespace: Namespace, json: Json)
                      (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(api, "group_info").withQuery(Query("namespace" -> namespace.get, "groupName" -> groupName)), json)

  def updateGroupInfo(groupName: String, namespace: Namespace, json: Json)
                      (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(api, "group_info").withQuery(Query("namespace" -> namespace.get, "groupName" -> groupName)), json)

  def deleteGroupInfo(groupName: String, namespace: Namespace)
                     (implicit ec: ExecutionContext): HttpRequest =
    Delete(Resource.uri(api, "group_info").withQuery(Query("namespace" -> namespace.get, "groupName" -> groupName)))
}
