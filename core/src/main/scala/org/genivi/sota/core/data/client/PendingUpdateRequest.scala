/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
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

case class PendingUpdateRequest(requestId: UUID, packageId: PackageId, installPos: Int,
                                status: UpdateStatus,
                                createdAt: Instant)

object PendingUpdateRequest {
  implicit val toResponseEncoder = GenericResponseEncoder {
    (u: UpdateRequest, updateStatus: UpdateStatus) =>
      PendingUpdateRequest(u.id, u.packageId, u.installPos, updateStatus, u.creationTime)
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
  implicit def fromRequestDecoder: RequestDecoder[ClientUpdateRequest, UpdateRequest, Namespace] =
    RequestDecoder { (req: ClientUpdateRequest, namespace: Namespace) =>
        UpdateRequest(req.id,
          namespace,
          req.packageId,
          req.creationTime,
          req.periodOfValidity,
          req.priority, req.signature, req.description, req.requestConfirmation)
    }
}
