/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import eu.timepit.refined.api.Refined
import org.genivi.sota.core.FakeDeviceRegistry
import org.genivi.sota.data.Device.DeviceName
import org.genivi.sota.data.{Device, Namespace, Namespaces}
import org.genivi.sota.resolver.data.Firmware

/**
 * Spec for Firmware REST actions
 */
class FirmwareResourceSpec extends ResourceWordSpec {

  val device: Device.Id = Device.Id(Refined.unsafeApply("069ae63b-b43e-423d-a50f-67dbdb2f1581"))

  override val deviceRegistry = {
    val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)
    deviceRegistry.addDevice(Device(Namespaces.defaultNs, device, DeviceName("device name")))
    deviceRegistry
  }

  "Firmware resource" should {
    "be able to accept installed software updates" in {
      installFirmwareOK(device, Set(), Set(Firmware(Namespace("default"),
        Refined.unsafeApply("ec1"): Firmware.Module, Refined.unsafeApply("1.0.0"): Firmware.FirmwareId, 42356329L)))
    }
  }
}
