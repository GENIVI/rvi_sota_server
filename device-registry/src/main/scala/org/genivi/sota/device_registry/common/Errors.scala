/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.common

import org.genivi.sota.data.GroupInfo
import org.genivi.sota.device_registry.db.GroupMemberRepository.GroupMember
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
    val SystemInfoAlreadyExists = ErrorCode("system_info_already_exists")
    val MissingGroupInfo = ErrorCode("missing_group_info")
    val GroupInfoAlreadyExists = ErrorCode("group_info_already_exists")
    val MemberAlreadyExists = ErrorCode("device_already_a_group_member")
  }

  val MissingDevice = RawError(Codes.MissingDevice, StatusCodes.NotFound, "device doesn't exist")
  val ConflictingDevice = RawError(Codes.ConflictingDevice, StatusCodes.Conflict,
    "deviceId or deviceName is already in use")
  val MissingSystemInfo = MissingEntity(classOf[SystemInfo])
  val ConflictingSystemInfo = EntityAlreadyExists(classOf[SystemInfo])

  val MissingGroupInfo = MissingEntity(classOf[GroupInfo])
  val ConflictingGroupInfo = EntityAlreadyExists(classOf[GroupInfo])
  val MemberAlreadyExists = EntityAlreadyExists(classOf[GroupMember])
}
