/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.client.NoContent
import org.genivi.sota.data.Device._
import org.genivi.sota.data._
import org.genivi.sota.device_registry.common.Errors._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


class FakeDeviceRegistry(namespace: Namespace)
                        (implicit system: ActorSystem, mat: ActorMaterializer)
  extends DeviceRegistry {

  val logger = Logging.getLogger(system, this)

  private val devices =  new ConcurrentHashMap[Uuid, Device]()
  private val systemInfo = new ConcurrentHashMap[Uuid, Json]()
  private val groups = new ConcurrentHashMap[Uuid, Seq[Uuid]]()
  private val installedPackages = new ConcurrentHashMap[Uuid, Set[PackageId]]()

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
  def createDevice(d: DeviceT)
                           (implicit ec: ExecutionContext): Future[Uuid] = {
    val uuid: Uuid = Uuid.generate()
    devices.put(uuid,
      Device(namespace = namespace,
        uuid = uuid,
        deviceName = d.deviceName,
        deviceId = d.deviceId,
        deviceType = d.deviceType,
        createdAt = Instant.now()))
    FastFuture.successful(uuid)
  }

  override def fetchDevice(ns: Namespace, uuid: Uuid)
                          (implicit ec: ExecutionContext): Future[Device] = fetchMyDevice(uuid)

  override def fetchMyDevice(uuid: Uuid)
                            (implicit ec: ExecutionContext): Future[Device] = {
    devices.asScala.get(uuid) match {
      case Some(d) => FastFuture.successful(d)
      case None => FastFuture.failed(MissingDevice)
    }
  }

  override def fetchDevicesInGroup(ns: Namespace, uuid: Uuid)
                                  (implicit ec: ExecutionContext): Future[Seq[Uuid]] = {
    groups.asScala.get(uuid) match {
      case Some(ds) => FastFuture.successful(ds)
      case None => FastFuture.successful(Seq())
    }
  }

  override def fetchByDeviceId
  (ns: Namespace, deviceId: DeviceId)
  (implicit ec: ExecutionContext): Future[Device] =
    devices
      .asScala
      .values
      .find(_.deviceId.contains(deviceId)) match {
      case Some(d) => FastFuture.successful(d)
      case None => FastFuture.failed(MissingDevice)
    }

  //This method only exists to facilitate testing, in production, this functionality
  //is performed by message passing
  def updateLastSeen(uuid: Uuid, seenAt: Instant = Instant.now)
  (implicit ec: ExecutionContext): Future[NoContent] = {
    devices.asScala.get(uuid).foreach { d =>
      devices.put(uuid, d.copy(lastSeen = Option(seenAt)))
    }

    FastFuture.successful(NoContent())
  }

  override def updateSystemInfo
  (uuid: Uuid, json: Json)
  (implicit ec: ExecutionContext): Future[NoContent] = {
    devices.asScala.get(uuid) match {
      case Some(_) =>
        systemInfo.put(uuid, json)
        FastFuture.successful(NoContent())
      case None =>
        FastFuture.failed(MissingDevice)
    }
  }

  def getSystemInfo
  (uuid: Uuid)
  (implicit ec: ExecutionContext): Future[Json] = {
    systemInfo.asScala.get(uuid) match {
      case Some(x) => FastFuture.successful(x)
      case None => FastFuture.failed(MissingDevice)
    }
  }

  def addDevice(device: Device): Device = {
    devices.put(device.uuid, device)
  }

  def addGroup(group: Uuid, devices: Seq[Uuid]): Unit = {
    groups.put(group, devices)
  }

  def setInstalledPackages(device: Uuid, packages: Seq[PackageId])
                          (implicit ec: ExecutionContext): Future[NoContent] = {
    installedPackages.compute(device, new BiFunction[Uuid, Set[PackageId], Set[PackageId]] {
      override def apply(t: Uuid, u: Set[PackageId]) = u match {
        case null => packages.toSet
        case others => others ++ packages
      }
    })

    FastFuture.successful(NoContent())
  }

  def setInstalledPackagesForDevices(data: Seq[(Uuid, Set[PackageId])])
                                    (implicit ec: ExecutionContext) : Future[NoContent] = {
    Future
      .sequence { data.map { case (uuid, pkgs) => setInstalledPackages(uuid, pkgs.toSeq) } }
      .map(_ => NoContent())
  }

  def isInstalled(uuid: Uuid, packageId: PackageId): Boolean =
    installedPackages.getOrDefault(uuid, Set.empty).contains(packageId)

  def affectedDevices(namespace: Namespace, packages: Set[PackageId])
                     (implicit ec: ExecutionContext): Future[Map[Uuid, Set[PackageId]]] = {
    Future.successful(installedPackages.asScala.map {
      case (device, pkgs) => (device, pkgs.intersect(packages))
    }.filter(_._2.nonEmpty).toMap)
  }
}
