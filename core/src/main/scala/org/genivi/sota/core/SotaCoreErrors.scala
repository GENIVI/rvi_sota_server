/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import org.genivi.sota.core.data.Campaign
import org.genivi.sota.core.data.Campaign.LaunchCampaignRequest
import org.genivi.sota.http.Errors.{EntityAlreadyExists, MissingEntity, RawError}
import org.genivi.sota.rest.{ErrorCode, ErrorCodes}

object SotaCoreErrors {
  object SotaCoreErrorCodes {
    val CantLaunchCampaign = ErrorCode("cant_launch_campaign")
    val CampaignLaunched = ErrorCode("campaign_launched")
    val ExternalResolverError = ErrorCode("external_resolver_error")
    val MissingDevice = ErrorCode("missing_device")
    val MissingPackage = ErrorCode("missing_package")
    val BlacklistedPackage = ErrorCode("blacklisted_package")
    val MissingImageForUpdate = ErrorCode("missing_image_to_queue")
    val DeviceLimitReached = ErrorCode("device_limit_reached")
    val DeviceNotActivated = ErrorCode("device_not_activated")
  }

  import StatusCodes._

  val BlacklistedPackage = RawError(SotaCoreErrorCodes.BlacklistedPackage, BadRequest, "package is blacklisted")
  val CantLaunchCampaign = RawError(SotaCoreErrorCodes.CantLaunchCampaign, BadRequest,
    "campaign is not ready to be launched")
  val CampaignLaunched = RawError(SotaCoreErrorCodes.CampaignLaunched, Locked, "campaign is already launched")
  val ConflictingCampaign = EntityAlreadyExists(classOf[Campaign])
  val MissingCampaign = MissingEntity(classOf[Campaign])
  val MissingPackage = RawError(SotaCoreErrorCodes.MissingPackage, NotFound, "package not found")
  val MissingUpdateSpec = RawError(ErrorCodes.MissingEntity, NotFound, "update spec not found")
  val MissingUpdateRequest = RawError(ErrorCodes.MissingEntity, NotFound, "update request not found")
  val MissingImageForUpdate = RawError(SotaCoreErrorCodes.MissingImageForUpdate, PreconditionFailed,
    "image for update not found")
  val DeviceLimitReached = RawError(SotaCoreErrorCodes.DeviceLimitReached, Forbidden, "limit of free devices reached")
  val DeviceNotActivated = RawError(SotaCoreErrorCodes.DeviceNotActivated, NotFound, "device has not been activated")
  val ConflictingLaunchCampaignRequest = EntityAlreadyExists(classOf[LaunchCampaignRequest])
  val MissingLaunchCampaignRequest = RawError(ErrorCodes.MissingEntity, NotFound, "launch campaign request not found")
  val InvalidDeltaFrom = RawError(ErrorCodes.ConflictingEntity, BadRequest, "delta from version must differ from " +
    "target version")
}
