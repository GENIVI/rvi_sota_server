/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

case class InstallRequest(
  id: Option[Long],
  installCampaignId: Long,
  packageId: Long,
  vin: Vehicle.IdentificationNumber,
  statusCode: InstallRequest.Status,
  errorMessage: Option[String]
)

object InstallRequest {
  object Status extends Enumeration {
    val NotProcessed = Value
    val Notified = Value
  }
  type Status = Status.Value

  def from(dependencyMap: Map[Vehicle, Set[Long]], campaignId: Long): Set[InstallRequest] = {
    dependencyMap.flatMap { case (vin, packageIds) =>
      packageIds.map(InstallRequest(None, campaignId, _, vin.vin, Status.NotProcessed, None))
    }.toSet
  }
}
