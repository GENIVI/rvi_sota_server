/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.common

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import org.genivi.sota.client.NoContent
import org.genivi.sota.data._

import scala.concurrent.{ExecutionContext, Future}
import Device._


trait DeviceRegistry {

  // TODO: Needs namespace
  def searchDevice
    (ns: Namespace, re: String Refined Regex)
    (implicit ec: ExecutionContext): Future[Seq[Device]]

  def listNamespace(ns: Namespace)
  (implicit ec: ExecutionContext): Future[Seq[Device]] =
    searchDevice(ns, Refined.unsafeApply(".*"))

  def fetchDevice
    (namespace: Namespace, uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Device]

  def fetchMyDevice
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Device]

  def fetchDevicesInGroup
    (namespace: Namespace, uuid: Uuid)
    (implicit ec: ExecutionContext): Future[PaginatedResult[Uuid]]

  def fetchByDeviceId
    (ns: Namespace, deviceId: DeviceId)
    (implicit ec: ExecutionContext): Future[Device]

  def updateSystemInfo
    (uuid: Uuid, json: Json)
    (implicit ec: ExecutionContext): Future[NoContent]

  def setInstalledPackages
    (device: Uuid, packages: Seq[PackageId])(implicit ec: ExecutionContext) : Future[NoContent]

  def affectedDevices(namespace: Namespace, packageIds: Set[PackageId])
                     (implicit ec: ExecutionContext): Future[Map[Uuid, Set[PackageId]]]
}
