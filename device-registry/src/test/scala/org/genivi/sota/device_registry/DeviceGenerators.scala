/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import eu.timepit.refined.api.Refined
import org.scalacheck.{Arbitrary, Gen}
import org.genivi.sota.data.Namespaces
import org.genivi.sota.device_registry.{Device, DeviceT}
import org.joda.time.DateTime


object DeviceGenerators {

  import Arbitrary._
  import Device._

  val genId: Gen[Id] = for {
    uuid <- Gen.uuid
  } yield Id(Refined.unsafeApply(uuid.toString))

  val genDeviceId: Gen[DeviceId] = for {
    id <- Gen.identifier
  } yield DeviceId(id)

  val genDeviceType: Gen[DeviceType] = for {
    t <- Gen.oneOf(DeviceType.values.toSeq)
  } yield t

  val genLastSeen: Gen[DateTime] = for {
    millis <- Gen.chooseNum[Long](0, 10000000000000L)
  } yield (new DateTime(millis))

  def genDeviceWith(deviceIdGen: Gen[DeviceId]): Gen[Device] = for {
    id <- genId
    deviceId <- Gen.option(deviceIdGen)
    deviceType <- genDeviceType
    lastSeen <- Gen.option(genLastSeen)
  } yield Device(Namespaces.defaultNs, id, deviceId, deviceType, lastSeen)

  val genDevice: Gen[Device] = genDeviceWith(genDeviceId)

  def genDeviceTWith(deviceIdGen: Gen[DeviceId]): Gen[DeviceT] = for {
    deviceId <- Gen.option(deviceIdGen)
    deviceType <- genDeviceType
  } yield DeviceT(deviceId, deviceType)

  val genDeviceT: Gen[DeviceT] = genDeviceTWith(genDeviceId)

  implicit lazy val arbId: Arbitrary[Id] = Arbitrary(genId)
  implicit lazy val arbDeviceId: Arbitrary[DeviceId] = Arbitrary(genDeviceId)
  implicit lazy val arbDeviceType: Arbitrary[DeviceType] = Arbitrary(genDeviceType)
  implicit lazy val arbLastSeen: Arbitrary[DateTime] = Arbitrary(genLastSeen)
  implicit lazy val arbDevice: Arbitrary[Device] = Arbitrary(genDevice)
  implicit lazy val arbDeviceT: Arbitrary[DeviceT] = Arbitrary(genDeviceT)

}
