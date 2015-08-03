/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import org.joda.time.DateTime

case class InstallCampaign(
  id: Option[Long],
  packageId: Long,
  priority: Int,
  startAfter: DateTime,
  endBefore: DateTime
)
