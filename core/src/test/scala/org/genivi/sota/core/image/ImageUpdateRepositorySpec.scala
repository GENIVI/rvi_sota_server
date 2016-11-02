/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import org.genivi.sota.core.db.image.{ImageRepositorySupport, ImageUpdateRepositorySupport}
import org.genivi.sota.core.{DatabaseSpec, DefaultPatience, ImageGenerators}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import scala.concurrent.ExecutionContext
import org.genivi.sota.data.GeneratorOps.GenSample

class ImageUpdateRepositorySpec extends FunSuite
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with ImageGenerators
  with DefaultPatience
  with ImageRepositorySupport
  with ImageUpdateRepositorySupport {

  implicit val ec = ExecutionContext.global
  implicit val _db = db


  test("persist on update does not overwrite id") {
    val (image, imageUpdate) = imageUpdateGenerator.generate

    val f = for {
      image <- imageRepository.persist(image.namespace, image.commit, image.ref, image.description, image.pullUri)
      inserted <- imageUpdateRepository.persist(imageUpdate.namespace, image.id, imageUpdate.device)
      updated <- imageUpdateRepository.persist(imageUpdate.namespace, image.id, imageUpdate.device)
    } yield (inserted, updated)

    val (inserted, updated) = f.futureValue

    inserted.id shouldBe updated.id
  }

  test("updates timestamp on update") {
    val (image, imageUpdate) = imageUpdateGenerator.generate

    val f = for {
      image <- imageRepository.persist(image.namespace, image.commit, image.ref, image.description, image.pullUri)
      inserted <- imageUpdateRepository.persist(imageUpdate.namespace, image.id, imageUpdate.device)
      updated <- imageUpdateRepository.persist(imageUpdate.namespace, image.id, imageUpdate.device)
    } yield (inserted, updated)

    val (inserted, updated) = f.futureValue

    inserted.createdAt shouldBe updated.createdAt

    withClue("inserted.updatedAt should be before updated.updatedAt") {
      inserted.updatedAt.isBefore(updated.updatedAt) shouldBe true
    }
  }
}
