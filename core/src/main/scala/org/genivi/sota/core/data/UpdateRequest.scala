/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import io.circe._
import java.util.UUID

import org.genivi.sota.data.{Interval, Namespace, UpdateStatus, Uuid}
import java.time.Instant
import java.time.Duration

/**
 * Domain object for an update request.
 * An update request refers to the intent to update a package on a number of
 * devices. It describes a single package, with a period of validity and priority
 * for the update.
 *
 * @param id A generated unique ID for this update request
 * @param packageUUid The uuid the package
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
 * @param installPos Derived from the corresponding field in [[UpdateSpec]]
 */
case class UpdateRequest(
  id: UUID,
  namespace: Namespace,
  packageUuid: UUID,
  creationTime: Instant,
  periodOfValidity: Interval,
  priority: Int,
  signature: String,
  description: Option[String],
  requestConfirmation: Boolean,
  installPos: Int = 0)

object UpdateRequest {

  def default(namespace: Namespace, packageUuid: UUID): UpdateRequest = {
    val updateRequestId = UUID.randomUUID()
    val now = Instant.now
    val defaultPeriod = Duration.ofDays(1)
    val defaultInterval = Interval(now, now.plus(defaultPeriod))
    val defaultPriority = 10

    UpdateRequest(updateRequestId, namespace, packageUuid,
      Instant.now, defaultInterval, defaultPriority, "", Some(""),
      requestConfirmation = false)
  }
}

import UpdateStatus._

/**
  * A combination (campaign, device, packages) --- which remain constant over time ---
  * along with the latest [[UpdateStatus]] (for that campaign on this device) --- which may change over time.
  * <br>
  * An UpdateSpec never gets deleted, just its [[status]] is rewritten.
  * Upon reaching Finished a row is added to [[InstallHistoryTable]] to record that fact
  * <br>
  * <ul>
  *   <li>The campaign (ie, [[UpdateRequest]]) that initiated this [[UpdateSpec]] is linked from here.</li>
  *   <li>Among pending [[UpdateSpec]] targeting a device an installation order is tracked by [[installPos]]</li>
  * </ul>
  *
  * @param request The campaign that these updates are a part of
  * @param device The vehicle to which these updates should be applied
  * @param status The status of the update
  * @param dependencies The packages to be installed
  * @param installPos Position in the installation queue, zero-based
  */
case class UpdateSpec(
  request: UpdateRequest,
  device: Uuid,
  status: UpdateStatus,
  dependencies: Set[Package],
  installPos: Int,
  creationTime: Instant,
  updateTime: Instant) {
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

  def default(request: UpdateRequest, device: Uuid): UpdateSpec = {
    UpdateSpec(request, device, UpdateStatus.Pending, Set.empty, 0, Instant.now, Instant.now)
  }
}

/**
 * A download that is sent via RVI. Consists of a set of packages.
 * @param packages A list of packages to install
 */
case class Download( packages: Vector[Package] )
