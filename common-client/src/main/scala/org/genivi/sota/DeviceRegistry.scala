/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.common

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.{Device, DeviceT}
import scala.concurrent.{ExecutionContext, Future}

import Device._


trait IDeviceRegistry {

  def searchDevice
    (re: String Refined Regex)
    (implicit ec: ExecutionContext): Future[Seq[Device]]

  def createDevice
    (device: DeviceT)
    (implicit ec: ExecutionContext): Future[Id]

  def fetchDevice
    (id: Id)
    (implicit ec: ExecutionContext): Future[Device]

  def fetchDeviceByDeviceId
    (id: DeviceId)
    (implicit ec: ExecutionContext): Future[Device]

  def updateDevice
    (id: Id, device: DeviceT)
    (implicit ec: ExecutionContext): Future[Unit]

  def deleteDevice
    (id: Id)
    (implicit ec: ExecutionContext): Future[Unit]

  def updateLastSeen
    (id: Id)
    (implicit ec: ExecutionContext): Future[Unit]

}
