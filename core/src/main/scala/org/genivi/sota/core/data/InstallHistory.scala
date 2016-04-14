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
 */
case class OperationResult(
  id : String,
  updateId: UUID,
  resultCode : Int,
  resultText : String
)

/**
 * Domain object for the install history of a VIN
 * @param id The Id in the database. Initialize to Option.None
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
