package org.genivi.sota.core

import org.joda.time.DateTime

case class InstallRequest(
  id: Option[Long],
  packageId: Long,
  priority: Int,
  startAfter: DateTime,
  endBefore: DateTime
)
