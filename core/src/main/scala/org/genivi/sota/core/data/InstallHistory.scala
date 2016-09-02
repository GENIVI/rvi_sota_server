/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.genivi.sota.data.Namespace
import java.time.Instant
import org.genivi.sota.data.{PackageId, Device}
import java.util.UUID

/**
 * Domain object for the update operation result
 * @param id The unique id of operation.
 * @param updateId Id of update this operation belongs to.
 * @param resultCode The status of operation.
 * @param resultText The description of operation.
 * @param device The device of the vehicle the operation was performed on
 * @param namespace The namespace for the given row
 */
case class OperationResult(
  id         : String,
  updateId   : UUID,
  resultCode : Int,
  resultText : String,
  device     : Device.Id,
  namespace  : Namespace,
  receivedAt : Instant)

object OperationResult {
  def from(rviOpResult: org.genivi.sota.core.rvi.OperationResult,
           updateRequestId: UUID,
           device    : Device.Id,
           namespace : Namespace
          ): OperationResult = {
    OperationResult(
      UUID.randomUUID().toString,
      updateRequestId,
      rviOpResult.result_code,
      rviOpResult.result_text,
      device    : Device.Id,
      namespace : Namespace,
      Instant.now
    )
  }
}

/**
 * Domain object for the install history of a device
 * @param id The Id in the database. Initialize to Option.None
 * @param namespace The namespace for the given row
 * @param device The device that this install history belongs to
 * @param updateId The Id of the update
 * @param packageId Id of package which belongs to this update.
 * @param success The outcome of the install attempt
 * @param completionTime The date the install was attempted
 */
case class InstallHistory(
  id             : Option[Long],
  namespace      : Namespace,
  device         : Device.Id,
  updateId       : UUID,
  packageId      : PackageId,
  success        : Boolean,
  completionTime : Instant
)
