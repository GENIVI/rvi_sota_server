/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import org.genivi.sota.core.data.UpdateStatus
import org.genivi.sota.core.db.image.DataType.{Image, ImageId, ImageUpdate, ImageUpdateId}
import org.genivi.sota.core.db.image.ImageUpdateRepositorySupport
import org.genivi.sota.data.{Namespace, Uuid}

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._

class ImageUpdateProcess()(implicit ec: ExecutionContext, db: Database) extends ImageUpdateRepositorySupport {
  def queue(ns: Namespace, image: ImageId, device: Uuid): Future[ImageUpdateId] = {
    imageUpdateRepository.persist(ns, image, device).map(_.id)
  }

  def findPendingFor(device: Uuid): Future[Seq[(ImageUpdate, Image)]] = {
    imageUpdateRepository.findForDevice(device, UpdateStatus.Pending)
  }
}
