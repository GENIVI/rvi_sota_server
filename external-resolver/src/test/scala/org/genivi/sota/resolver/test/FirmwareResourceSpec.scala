/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import java.time.Instant

import eu.timepit.refined.api.Refined
import org.genivi.sota.core.FakeDeviceRegistry
import org.genivi.sota.data._
import org.genivi.sota.resolver.firmware.Firmware

/**
 * Spec for Firmware REST actions
 */
class FirmwareResourceSpec extends ResourceWordSpec {

  val device: Uuid = Uuid(Refined.unsafeApply("069ae63b-b43e-423d-a50f-67dbdb2f1581"))

  override val deviceRegistry = {
    val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)
    deviceRegistry.addDevice(Device(Namespaces.defaultNs, device, Refined.unsafeApply("device name"), createdAt = Instant.now()))
    deviceRegistry
  }

  "Firmware resource" should {
    "be able to accept installed software updates" in {
      installFirmwareOK(device, Set(), Set(Firmware(Namespace("default"),
        Refined.unsafeApply("ec1"): Firmware.Module, Refined.unsafeApply("1.0.0"): Firmware.FirmwareId, 42356329L)))
    }
  }
}
