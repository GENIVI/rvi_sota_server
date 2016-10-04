/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.common

import org.genivi.sota.data.GroupInfo
import org.genivi.sota.device_registry.db.SystemInfoRepository.SystemInfo
import org.genivi.sota.http.Errors.{EntityAlreadyExists, MissingEntity, RawError}

object Errors {
  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.Directives.complete
  import akka.http.scaladsl.server.ExceptionHandler.PF
  import scala.util.control.NoStackTrace
  import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
  import org.genivi.sota.marshalling.CirceMarshallingSupport._

  object Codes {
    val MissingDevice = ErrorCode("missing_device")
    val ConflictingDevice = ErrorCode("conflicting_device")
    val MissingSystemInfo = ErrorCode("missing_system_info")
    val SystemInfoAlreadyExists = ErrorCode("system_info_already_exists")
    val MissingGroupInfo = ErrorCode("missing_group_info")
    val GroupInfoAlreadyExists = ErrorCode("group_info_already_exists")
  }

  val MissingDevice = RawError(Codes.MissingDevice, StatusCodes.NotFound, "device doesn't exist")
  val ConflictingDevice = RawError(Codes.ConflictingDevice, StatusCodes.Conflict,
    "deviceId or deviceName is already in use")
  val MissingSystemInfo = RawError(Codes.MissingSystemInfo, StatusCodes.NotFound,
    "system info doesn't exist for this uuid")
  val ConflictingSystemInfo = EntityAlreadyExists(classOf[SystemInfo])

  val MissingGroupInfo = MissingEntity(classOf[GroupInfo])
  val ConflictingGroupInfo = EntityAlreadyExists(classOf[GroupInfo])
}
