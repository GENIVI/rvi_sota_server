/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.http.scaladsl.client.RequestBuilding.{Delete, Get, Post, Put}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, Uri}
import cats.Show
import io.circe.generic.auto._
import io.circe.Json
import org.genivi.sota.data.{Device, DeviceT, GroupInfo, Namespace, Uuid}
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
  import GroupInfo.Name

  val api = "devices"

  def fetchDevice(uuid: Uuid)
                 (implicit s: Show[Uuid]): HttpRequest =
    Get(Resource.uri(api, s.show(uuid)))

  def searchDevice(namespace: Namespace, regex: String): HttpRequest =
    Get(Resource.uri(api).withQuery(Query("namespace" -> namespace.get, "regex" -> regex)))

  def fetchByDeviceId(namespace: Namespace, deviceId: Device.DeviceId)
                     (implicit s: Show[Device.DeviceId]): HttpRequest =
    Get(Resource.uri(api).withQuery(Query("namespace" -> namespace.get, "deviceId" -> s.show(deviceId))))

  def updateDevice(uuid: Uuid, device: DeviceT)
                  (implicit s: Show[Uuid], ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(api, s.show(uuid)), device)

  def createDevice(device: DeviceT)
                  (implicit ec: ExecutionContext): HttpRequest = {
    Post(Resource.uri(api), device)
  }

  def deleteDevice(uuid: Uuid)
                  (implicit s: Show[Uuid]): HttpRequest =
    Delete(Resource.uri(api, s.show(uuid)))

  def updateLastSeen(uuid: Uuid)
                    (implicit s: Show[Uuid]): HttpRequest =
    Post(Resource.uri(api, s.show(uuid), "ping"))

  def fetchSystemInfo(uuid: Uuid)
                     (implicit s: Show[Uuid]): HttpRequest =
    Get(Resource.uri(api, s.show(uuid), "system_info"))

  def createSystemInfo(uuid: Uuid, json: Json)
                      (implicit s: Show[Uuid], ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(api, s.show(uuid),"system_info"), json)

  def updateSystemInfo(uuid: Uuid, json: Json)
                      (implicit s: Show[Uuid], ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(api, s.show(uuid),"system_info"), json)

  def listGroups(): HttpRequest = Get(Resource.uri(api, "group_info"))

  def fetchGroupInfo(id: Uuid): HttpRequest = Get(Resource.uri(api, id.underlying.get, "group_info"))

  def createGroupInfo(id: Uuid, groupName: Name, json: Json)
                      (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(api, id.underlying.get, "group_info").withQuery(Query("groupName" -> groupName.get)), json)

  def updateGroupInfo(id: Uuid, groupName: Name, json: Json)
                      (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(api, id.underlying.get, "group_info").withQuery(Query("groupName" -> groupName.get)), json)

  def renameGroup(id: Uuid, newGroupName: Name)(implicit ec: ExecutionContext): HttpRequest = {
    Put(Resource.uri(api, id.underlying.get, "group_info", "rename").withQuery(Query("groupName" -> newGroupName.get)))
  }
}
