/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data.client

import java.util.UUID

import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.data.Namespace
import org.genivi.sota.data.PackageId
import java.time.Instant

import org.genivi.sota.core.data.UpdateStatus.UpdateStatus

import scala.language.implicitConversions
import org.genivi.sota.data.Interval
import shapeless.{::, HNil}

case class PendingUpdateRequest(requestId: UUID, packageId: PackageId, installPos: Int,
                                status: UpdateStatus,
                                createdAt: Instant,
                                updatedAt: Instant)

object PendingUpdateRequest {
  implicit val toResponseEncoder = GenericResponseEncoder {
    (u: UpdateRequest, updateStatus: UpdateStatus, packageId: PackageId, updatedAt: Instant) =>
      PendingUpdateRequest(u.id, packageId, u.installPos, updateStatus, u.creationTime, updatedAt)
  }
}

case class ClientUpdateRequest(id: UUID,
                         packageId: PackageId,
                         creationTime: Instant = Instant.now,
                         periodOfValidity: Interval,
                         priority: Int,
                         signature: String,
                         description: Option[String],
                         requestConfirmation: Boolean)

object ClientUpdateRequest {
  implicit val toResponseEncoder = GenericResponseEncoder { (req: UpdateRequest, packageId: PackageId) =>
    ClientUpdateRequest(req.id, packageId,
      req.creationTime, req.periodOfValidity, req.priority, req.signature, req.description, req.requestConfirmation)
  }

  implicit val fromRequestDecoder =
    GenericArgsDecoder { (req: ClientUpdateRequest, packageUuid: UUID) =>
      UpdateRequest(req.id,
        packageUuid,
        req.creationTime,
        req.periodOfValidity,
        req.priority, req.signature, req.description, req.requestConfirmation)
    }
}
