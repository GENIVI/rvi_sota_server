/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import org.genivi.sota.core.data.Campaign
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
  }

  val BlacklistedPackage = RawError(SotaCoreErrorCodes.BlacklistedPackage, StatusCodes.BadRequest,
    "package is blacklisted")
  val CantLaunchCampaign = RawError(SotaCoreErrorCodes.CantLaunchCampaign, StatusCodes.BadRequest,
                                    "campaign is not readly to be launched")
  val CampaignLaunched = RawError(SotaCoreErrorCodes.CampaignLaunched, StatusCodes.Locked,
                                  "campaign is already launched, can't edit")
  val ConflictingCampaign = EntityAlreadyExists(classOf[Campaign])
  val MissingCampaign = MissingEntity(classOf[Campaign])
  val MissingPackage = RawError(SotaCoreErrorCodes.MissingPackage, StatusCodes.NotFound, "package not found")
  val MissingUpdateSpec = RawError(ErrorCodes.MissingEntity, StatusCodes.NotFound, "update spec not found")
}
