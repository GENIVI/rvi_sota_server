/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import java.time.Instant
import java.util.UUID._
import java.util.concurrent.ConcurrentHashMap
import org.genivi.sota.data.Device._
import cats.syntax.show._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import io.circe.Json
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.Device._
import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.device_registry.common.Errors._
import scala.concurrent.JavaConversions._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


import scala.concurrent.{ExecutionContext, Future}

class FakeDeviceRegistry(namespace: Namespace)
                        (implicit system: ActorSystem, mat: ActorMaterializer)
  extends DeviceRegistry {

  val logger = Logging.getLogger(system, this)

  private val devices =  new ConcurrentHashMap[Device.Id, Device]()
  private val systemInfo = new ConcurrentHashMap[Device.Id, Json]()

  override def searchDevice
  (ns: Namespace, re: String Refined Regex)
  (implicit ec: ExecutionContext): Future[Seq[Device]] = {
    FastFuture.successful(
      devices
        .values()
        .asScala
        .filter(_.deviceId.isDefined)
        .filter(d => re.get.r.findFirstIn(d.deviceId.map(_.show).getOrElse("")).isDefined)
        .toSeq
    )
  }

  // TODO: handle conflicts on deviceId
  override def createDevice(d: DeviceT)
                           (implicit ec: ExecutionContext): Future[Id] = {
    val id: Id = Id(Refined.unsafeApply(randomUUID.toString))
    devices.put(id,
      Device(namespace = namespace,
        id = id,
        deviceName = d.deviceName,
        deviceId = d.deviceId,
        deviceType = d.deviceType))
    FastFuture.successful(id)
  }

  override def fetchDevice(id: Id)
                          (implicit ec: ExecutionContext): Future[Device] = {
    devices.asScala.get(id) match {
      case Some(d) => FastFuture.successful(d)
      case None => FastFuture.failed(MissingDevice)
    }
  }

  override def fetchByDeviceId
  (ns: Namespace, id: DeviceId)
  (implicit ec: ExecutionContext): Future[Device] =
    devices
      .asScala
      .values
      .find(_.deviceId.contains(id)) match {
      case Some(d) => FastFuture.successful(d)
      case None => FastFuture.failed(MissingDevice)
    }

  // TODO: handle conflicts on deviceId
  override def updateDevice
  (id: Id, device: DeviceT)
  (implicit ec: ExecutionContext): Future[Unit] = {
    devices.asScala.get(id) match {
      case Some(d) =>
        d.copy(deviceId = device.deviceId, deviceName = device.deviceName, deviceType = device.deviceType)
      case None =>
    }

    Future.successful(())
  }

  override def deleteDevice
  (id: Id)
  (implicit ec: ExecutionContext): Future[Unit] = {
    devices.remove(id)
    FastFuture.successful(())
  }

  override def updateLastSeen(id: Id, seenAt: Instant = Instant.now)
  (implicit ec: ExecutionContext): Future[Unit] = {
    devices.asScala.get(id).foreach { d =>
      devices.put(id, d.copy(lastSeen = Option(seenAt)))
    }

    FastFuture.successful(())
  }

  override def updateSystemInfo
  (id: Id, json: Json)
  (implicit ec: ExecutionContext): Future[Unit] = {
    devices.asScala.get(id) match {
      case Some(_) =>
        systemInfo.put(id, json)
        FastFuture.successful(())
      case None =>
        FastFuture.failed(MissingDevice)
    }
  }

  override def getSystemInfo
  (id: Id)
  (implicit ec: ExecutionContext): Future[Json] = {
    systemInfo.asScala.get(id) match {
      case Some(x) => FastFuture.successful(x)
      case None => FastFuture.failed(MissingDevice)
    }
  }

  def addDevice(device: Device): Device = {
    devices.put(device.id, device)
  }
}
