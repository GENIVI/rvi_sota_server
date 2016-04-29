/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data.response

import java.util.UUID

import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.data.response.ResponseConversions.Response
import org.genivi.sota.data.PackageId
import org.joda.time.DateTime

import scala.language.implicitConversions

case class PendingUpdateResponse(requestId: UUID, packageId: PackageId, createdAt: DateTime) extends Response

object PendingUpdateResponse {
  implicit def toResponse(v: UpdateRequest): PendingUpdateResponse = {
    PendingUpdateResponse(v.id, v.packageId, v.creationTime)
  }
}
