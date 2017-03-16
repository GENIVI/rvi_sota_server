/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import java.time.Instant

import org.genivi.sota.data.DeviceStatus.DeviceStatus
import org.slf4j.LoggerFactory

object DeviceStatus extends CirceEnum with SlickEnum {
  type DeviceStatus = Value

  val NotSeen, Error, UpToDate, Outdated = Value
}

case class DeviceUpdateStatus(device: Uuid, status: DeviceStatus, lastSeen: Option[Instant])

object DeviceSearchCommon {
  import DeviceStatus._
  import UpdateStatus._

  private lazy val logger = LoggerFactory.getLogger(this.getClass)

  def currentDeviceStatus(lastSeen: Option[Instant], updateStatuses: Seq[(Instant, UpdateStatus)]): DeviceStatus = {

    if(lastSeen.isEmpty) {
      NotSeen
    } else {
      val statuses = updateStatuses.sortBy(_._1).reverse.map(_._2)

      if(statuses.headOption.contains(UpdateStatus.Failed)) {
        Error
      } else if(!statuses.forall(s => List(Canceled, Finished, Failed).contains(s))) {
        Outdated
      } else {
        UpToDate
      }
    }
  }

}
