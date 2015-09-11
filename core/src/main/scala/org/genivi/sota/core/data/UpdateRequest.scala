/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import java.util.UUID
import cats.Foldable
import org.joda.time.{Interval, DateTime}

case class UpdateRequest( id: UUID, packageId: PackageId, creationTime: DateTime, periodOfValidity: Interval, priority: Int )

object UpdateStatus extends Enumeration {
  type UpdateStatus = Value

  val Pending, InFlight, Canceled, Failed, Finished = Value
}

import UpdateStatus._

case class UpdateSpec( request: UpdateRequest, vin: Vehicle.IdentificationNumber, status: UpdateStatus, dependencies: Set[Package] ) {
  def size : Long = dependencies.foldLeft(0L)( _ + _.size)
}

case class Download( packages: Vector[Package] )
