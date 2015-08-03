/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

case class InstallRequest(
  id: Option[Long],
  installCampaignId: Long,
  packageId: Long,
  vin: String,
  statusCode: Char,
  errorMessage: Option[String]
)

object InstallRequest {
  def from(dependencyMap: Map[Vin, Set[Long]], campaignId: Long): Set[InstallRequest] = {
    dependencyMap.flatMap { case (vin, packageIds) =>
      packageIds.map(InstallRequest(None, campaignId, _, vin.vin, '0', None))
    }.toSet
  }
}
