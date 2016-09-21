/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.genivi.sota.data.Namespace
import java.time.Instant
import org.genivi.sota.data.{Device, PackageId, Uuid}
import java.util.UUID

import org.genivi.sota.core.data.client.GenericResponseEncoder

/**
 * Domain object for the update operation result
 * @param id The unique id of operation.
 * @param updateId Id of update this operation belongs to.
 * @param resultCode The status of operation.
 * @param resultText The description of operation.
 * @param device The device of the vehicle the operation was performed on
 */
case class OperationResult(
  id         : String,
  updateId   : UUID,
  resultCode : Int,
  resultText : String,
  device     : Uuid,
  receivedAt : Instant)

object OperationResult {
  def from(rviOpResult: org.genivi.sota.core.rvi.OperationResult,
           updateRequestId: UUID,
           device: Uuid
          ): OperationResult = {
    OperationResult(
      UUID.randomUUID().toString,
      updateRequestId,
      rviOpResult.result_code,
      rviOpResult.result_text,
      device: Uuid,
      Instant.now
    )
  }
}

case class InstallHistory(
  id             : Option[Long],
  device         : Uuid,
  updateId       : UUID,
  packageUuid    : UUID,
  success        : Boolean,
  completionTime : Instant
)

case class ClientInstallHistory(
  id             : Option[Long],
  device         : Uuid,
  updateId       : UUID,
  packageId      : PackageId,
  success        : Boolean,
  completionTime : Instant
)

object ClientInstallHistory {
  implicit val toResponseEncoder = GenericResponseEncoder {
    (ih: InstallHistory, packageId: PackageId) =>
      ClientInstallHistory(
        ih.id,
        ih.device,
        ih.updateId,
        packageId,
        ih.success,
        ih.completionTime
      )
  }
}
