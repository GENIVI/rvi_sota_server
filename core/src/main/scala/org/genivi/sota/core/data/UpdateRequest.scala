/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import java.util.UUID

import org.joda.time.{DateTime, Interval}
import io.circe._
import org.genivi.sota.data.{PackageId, Vehicle}

/**
 * Domain object for an update request.
 * An update request refers to the intent to update a package on a number of
 * VINs. It describes a single package, with a period of validity and priority
 * for the update.
 * @param id A generated unique ID for this update request
 * @param packageId The name and version of the package
 * @param creationTime When this update request was entered into SOTA
 * @param periodOfValidity The start and end times when this update may be
 *                         installed. The install won't be attempted before
 *                         this point and will fail it it hasn't been started
 *                         after the interval expires.
 * @param priority The priority. priority == 1 items are installed before
 *                 priority == 2.
 * @param signature Signature for this updates Id
 * @param description A descriptive text of the available update.
 * @param requestConfirmation Flag to indicate if a user confirmation of the package is required.
 */
case class UpdateRequest(
  id: UUID,
  packageId: PackageId,
  creationTime: DateTime,
  periodOfValidity: Interval,
  priority: Int,
  signature: String,
  description: Option[String],
  requestConfirmation: Boolean)

/**
 * The states that an update may be in.
 * Updates start in Pending state, then go to InFlight, then either Failed or
 * Finished. At any point before the Failed or Finished state it may transfer
 * to the Canceled state when a user cancels the operation
 */
object UpdateStatus extends Enumeration {
  type UpdateStatus = Value

  val Pending, InFlight, Canceled, Failed, Finished = Value
}

import UpdateStatus._

/**
 * A set of package updates to apply to a single VIN.
 * @param request The update campaign that these updates are a part of
 * @param vin The vehicle to which these updates should be applied
 * @param status The status of the update
 * @param dependencies The packages to be installed
 */
case class UpdateSpec(
  request: UpdateRequest,
  vin: Vehicle.Vin,
  status: UpdateStatus,
  dependencies: Set[Package] ) {

  /**
   * The combined size (in bytes) of all the software updates in this package
   */
  def size : Long = dependencies.foldLeft(0L)( _ + _.size)
}

/**
 * Implicits to implement a JSON encoding/decoding for UpdateStatus
 * @see [[http://circe.io/]]
 */
object UpdateSpec {
  implicit val updateStatusEncoder : Encoder[UpdateStatus] = Encoder[String].contramap(_.toString)
  implicit val updateStatusDecoder : Decoder[UpdateStatus] = Decoder[String].map(UpdateStatus.withName)
}

/**
 * A download that is sent via RVI. Consists of a set of packages.
 * @param packages A list of packages to install
 */
case class Download( packages: Vector[Package] )
