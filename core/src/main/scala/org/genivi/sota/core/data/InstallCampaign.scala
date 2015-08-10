/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import org.joda.time.DateTime

case class InstallCampaign(
  id: Option[Long],
  packageId: Long,
  priority: Int,
  startAfter: DateTime,
  endBefore: DateTime
)

object InstallCampaign {
  import org.genivi.sota.core.DateTimeJsonProtocol._
  implicit val installCampaignFormat = jsonFormat5(InstallCampaign.apply)
}
