/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.common

import java.time.Instant
import io.circe.Json

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.{Device, DeviceT, Namespace}

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
  (implicit ec: ExecutionContext): Future[Id]

  def fetchDevice
    (id: Id)
    (implicit ec: ExecutionContext): Future[Device]

  def fetchByDeviceId
    (ns: Namespace, id: DeviceId)
    (implicit ec: ExecutionContext): Future[Device]

  def updateDevice
    (id: Id, device: DeviceT)
    (implicit ec: ExecutionContext): Future[Unit]

  def deleteDevice
    (id: Id)
    (implicit ec: ExecutionContext): Future[Unit]

  def updateLastSeen
    (id: Id, seenAt: Instant = Instant.now)
    (implicit ec: ExecutionContext): Future[Unit]

  def updateSystemInfo
    (id: Id, json: Json)
    (implicit ec: ExecutionContext): Future[Unit]

  def getSystemInfo
    (id:Id)
    (implicit ec: ExecutionContext): Future[Json]
}
