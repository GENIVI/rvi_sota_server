/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import java.util.UUID
import org.joda.time.{Interval, DateTime}

case class UpdateRequest( id: UUID, packageId: PackageId, creationTime: DateTime, periodOfValidity: Interval, priority: Int )

object UpdateStatus extends Enumeration {
  type UpdateStatus = Value

  val Pending, InFlight, Canceled, Failed, Finished = Value
}

import UpdateStatus._

case class UpdateSpec( request: UpdateRequest, vin: Vehicle.IdentificationNumber, chunkSize: Int, status: UpdateStatus, downloads: Vector[Download] )

case class Download( packages: Vector[Package] )
