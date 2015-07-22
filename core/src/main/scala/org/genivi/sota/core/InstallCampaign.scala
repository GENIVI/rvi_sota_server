package org.genivi.sota.core

import org.joda.time.DateTime

case class InstallCampaign(
  id: Option[Long],
  packageId: Long,
  priority: Int,
  startAfter: DateTime,
  endBefore: DateTime
)
