/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.common

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import java.time.Instant
import org.genivi.sota.data.{Device, DeviceT, Namespace, Uuid}
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

  def createDevice
    (device: DeviceT)
    (implicit ec: ExecutionContext): Future[Uuid]

  def fetchDevice
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Device]

  def fetchMyDevice
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Device]

  def fetchDevicesInGroup
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Seq[Uuid]]

  def fetchGroup
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Seq[Uuid]]

  def fetchByDeviceId
    (ns: Namespace, deviceId: DeviceId)
    (implicit ec: ExecutionContext): Future[Device]

  def updateDevice
    (uuid: Uuid, device: DeviceT)
    (implicit ec: ExecutionContext): Future[Unit]

  def deleteDevice
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Unit]

  def updateLastSeen
    (uuid: Uuid, seenAt: Instant = Instant.now)
    (implicit ec: ExecutionContext): Future[Unit]

  def updateSystemInfo
    (uuid: Uuid, json: Json)
    (implicit ec: ExecutionContext): Future[Unit]

  def getSystemInfo
    (uuid: Uuid)
    (implicit ec: ExecutionContext): Future[Json]
}
