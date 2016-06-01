/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.joda.time.DateTime
import java.util.UUID

/**
 * Domain object for the update operation result
 * @param id The unique id of operation.
 * @param updateId Id of update this operation belongs to.
 * @param resultCode The status of operation.
 * @param resultText The description of operation.
 * @param vin The vin of the vehicle the operation was performed on
 * @param namespace The namespace for the given row
 */
case class OperationResult(
  id        : String,
  updateId  : UUID,
  resultCode: Int,
  resultText: String,
  vin       : Vehicle.Vin,
  namespace : Namespace,
  receivedAt: DateTime
)
object OperationResult {
  def from(rviOpResult: org.genivi.sota.core.rvi.OperationResult,
           updateRequestId: UUID,
           vin       : Vehicle.Vin,
           namespace : Namespace
          ): OperationResult = {
    OperationResult(
      UUID.randomUUID().toString,
      updateRequestId,
      rviOpResult.result_code,
      rviOpResult.result_text,
      vin       : Vehicle.Vin,
      namespace : Namespace,
      DateTime.now
    )
  }
}

/**
 * Domain object for the install history of a VIN
 * @param id The Id in the database. Initialize to Option.None
 * @param namespace The namespace for the given row
 * @param vin The VIN that this install history belongs to
 * @param updateId The Id of the update
 * @param packageId Id of package which belongs to this update.
 * @param success The outcome of the install attempt
 * @param completionTime The date the install was attempted
 */
case class InstallHistory(
  id            : Option[Long],
  namespace     : Namespace,
  vin           : Vehicle.Vin,
  updateId      : UUID,
  packageId     : PackageId,
  success       : Boolean,
  completionTime: DateTime
)
