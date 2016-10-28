/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

// scalastyle:off

package org.genivi.sota.core.db.image

import java.time.Instant

import org.genivi.sota.core.data.UpdateStatus._
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.db.SlickAnyVal._
import slick.driver.MySQLDriver.api._

object ImageSchema {
  import DataType._
  import org.genivi.sota.refined.SlickRefined._
  import org.genivi.sota.core.db.UpdateSpecs.UpdateStatusColumn
  import org.genivi.sota.db.SlickExtensions._


  class ImageTable(tag: Tag) extends Table[Image](tag, "Image") {
    def namespace = column[Namespace]("namespace")
    def id = column[ImageId]("uuid")
    def commit = column[Commit]("commit")
    def ref = column[RefName]("ref")
    def description = column[String]("description")
    def pullUrl = column[PullUri]("pull_url")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")

    def pk = primaryKey("pk_image", id)

    def * = (namespace, id, commit, ref, description, pullUrl, createdAt, updatedAt) <> ((Image.apply _).tupled, Image.unapply)
  }

  protected[image] val images = TableQuery[ImageTable]

  class ImageUpdateTable(tag: Tag) extends Table[ImageUpdate](tag, "ImageUpdate") {
    def id = column[ImageUpdateId]("uuid")
    def namespace = column[Namespace]("namespace")
    def imageId = column[ImageId]("image_uuid")
    def device = column[Uuid]("device_uuid")
    def status = column[UpdateStatus]("status")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")

    def pk = primaryKey("pk_image_update", id)

    def imagesFk = foreignKey("fk_image_update_image", imageId, images)(_.id)

    def * = (namespace, id, imageId, device, status, createdAt, updatedAt) <> ((ImageUpdate.apply _).tupled, ImageUpdate.unapply)
  }

  protected[image] val imageUpdates = TableQuery[ImageUpdateTable]
}
