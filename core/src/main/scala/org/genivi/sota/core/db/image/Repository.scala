/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.db.image

import java.time.Instant

import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.core.data.UpdateStatus
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import org.genivi.sota.data.{Namespace, Uuid}
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}


trait ImageRepositorySupport {
  def imageRepository(implicit ec: ExecutionContext, db: Database) = new ImageRepository
}

protected class ImageRepository()(implicit ec: ExecutionContext, db: Database) {
  import DataType._
  import ImageSchema._
  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.db.SlickAnyVal._
  import org.genivi.sota.refined.SlickRefined._

  def persist(ns: Namespace, commit: Commit, ref: RefName, desc: String, pullUri: PullUri): Future[Image] = {
    val now = Instant.now()
    val image = Image(ns, ImageId.generate(), commit, ref, desc, pullUri, now, now)

    images

    val findQuery = images
      .filter(_.namespace === ns)
      .filter(_.commit === commit)
      .filter(_.ref === ref)

    def update(old: Image): DBIO[Image] = {
      val updated = old.copy(commit = commit, ref = ref,
        description = desc, pullUri = pullUri, updatedAt = now)

      findQuery.update(updated).map(_ => updated)
    }

    val io = findQuery.result.headOption.flatMap {
      case Some(old) => update(old)
      case None => (images += image).map(_ => image)
    }

    db.run(io.transactionally)
  }

  def findAll(ns: Namespace): Future[Seq[Image]] =
    db.run(images.filter(_.namespace === ns).result)
}

trait ImageUpdateRepositorySupport {
  def imageUpdateRepository(implicit ec: ExecutionContext, db: Database) = new ImageUpdateRepository
}

protected class ImageUpdateRepository()(implicit ec: ExecutionContext, db: Database) {
  import DataType._
  import ImageSchema._
  import org.genivi.sota.core.db.UpdateSpecs.UpdateStatusColumn
  import org.genivi.sota.db.SlickExtensions._
  import SotaCoreErrors.MissingImageForUpdate
  import org.genivi.sota.db.SlickAnyVal._

  def persist(ns: Namespace, imageId: ImageId, device: Uuid): Future[ImageUpdate] = {
    val now = Instant.now()
    val imageUpdate = ImageUpdate(ns, ImageUpdateId.generate(), imageId, device, UpdateStatus.Pending, now, now)

    val findQuery = imageUpdates
      .filter(_.imageId === imageId)
      .filter(_.device === device)

    def update(v: ImageUpdate): DBIO[ImageUpdate] = {
      val updated = v.copy(imageId = imageId, device = device, updatedAt = now)
      findQuery.update(updated).map(_ => updated)
    }

    val io = findQuery.result.headOption.flatMap {
      case Some(v) => update(v)
      case None => (imageUpdates += imageUpdate).map(_ => imageUpdate)
    }

    db.run(io.handleIntegrityErrors(MissingImageForUpdate).transactionally)
  }

  def findForDevice(device: Uuid, status: UpdateStatus): Future[Seq[(ImageUpdate, Image)]] = {
    val io =
      imageUpdates
        .filter(_.device === device)
        .filter(_.status === status)
        .join(images).on(_.imageId === _.id)

    db.run(io.result)
  }
}
