/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import akka.http.scaladsl.model.StatusCodes._
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, DeviceT, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._

class PublicCredentialsResourceSpec extends ResourcePropSpec {
  import Device._

  property("GET requests fails on non-existent device") {
    forAll { (uuid:Uuid) =>
      fetchPublicCredentials(uuid) ~> route ~> check { status shouldBe NotFound }
    }
  }

  property("GET request after PUT yields same credentials") {
    forAll { (deviceId: DeviceId, creds: Array[Byte]) =>
      val uuid = updatePublicCredentialsOk(deviceId, creds)

      fetchPublicCredentialsOk(uuid) shouldBe creds

      deleteDeviceOk(uuid)
    }
  }

  property("PUT uses existing uuid if device exists") {
    forAll { (devId: DeviceId, mdevT: DeviceT, creds: Array[Byte]) =>
      val devT = mdevT.copy(deviceId = Some(devId))
      val uuid = createDeviceOk(devT)
      updatePublicCredentials(devId, creds) ~> route ~> check {
        status shouldBe OK
        val uuid2 = responseAs[Uuid]
        uuid2 shouldBe uuid
      }

      // updatePublicCredentials didn't change the device
      fetchDevice(uuid) ~> route ~> check {
        status shouldBe OK
        val dev = responseAs[Device]
        dev.deviceName shouldBe devT.deviceName
        dev.deviceId   shouldBe devT.deviceId
        dev.deviceType shouldBe devT.deviceType
      }

      deleteDeviceOk(uuid)
    }
  }

  property("Latest PUT is the one that wins") {
    forAll { (deviceId: DeviceId, creds1: Array[Byte], creds2: Array[Byte]) =>
      val uuid = updatePublicCredentialsOk(deviceId, creds1)
      updatePublicCredentialsOk(deviceId, creds2)

      fetchPublicCredentialsOk(uuid) shouldBe creds2

      deleteDeviceOk(uuid)
    }
  }
}
