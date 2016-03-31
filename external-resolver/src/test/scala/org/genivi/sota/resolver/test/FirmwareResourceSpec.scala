/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import eu.timepit.refined.api.Refined
import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.vehicles.Vehicle

/**
 * Spec for Firmware REST actions
 */
class FirmwareResourceSpec extends ResourceWordSpec {

  "Firmware resource" should {
    "be able to accept installed software updates" in {
      val vin: Vehicle.Vin = Refined.unsafeApply("TESTVAN0123456789")
      addVehicleOK(vin)
      installFirmwareOK(vin, Set(), Set((Refined.unsafeApply("ec1"): Firmware.Module,
          Refined.unsafeApply("1.0.0"): Firmware.FirmwareId, 42356329L)))
    }
  }
}
