/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import org.genivi.sota.core.db.image.ImageRepositorySupport
import org.genivi.sota.core.{DatabaseSpec, DefaultPatience, ImageGenerators}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.data.GeneratorOps.GenSample


class ImageRepositorySpec extends FunSuite
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with ImageGenerators
  with DefaultPatience
  with ImageRepositorySupport {

  implicit val _db = db
  implicit val ec = ExecutionContext.global

  test("persist on update does not overwrite id") {
    val image = imageGenerator.generate

    val f = for {
      inserted <- imageRepository.persist(image.namespace, image.commit, image.ref, image.description, image.pullUri)
      updated <- imageRepository.persist(image.namespace, image.commit, image.ref, "Other description", image.pullUri)
    } yield (inserted, updated)

    val (inserted, updated) = f.futureValue

    inserted.id shouldBe updated.id
    inserted.description shouldNot be(updated.description)
  }

  test("persist updates entity if exists") {
    val image = imageGenerator.generate

    val f = for {
      _ <- imageRepository.persist(image.namespace, image.commit, image.ref, image.description, image.pullUri)
      _ <- imageRepository.persist(image.namespace, image.commit, image.ref, "Other description", image.pullUri)
      images <- imageRepository.findAll(image.namespace)
    } yield images

    val queue = f.futureValue

    queue.count { i => i.commit == image.commit } shouldBe 1

    queue.find(_.commit == image.commit).map(_.description) should contain("Other description")
  }

  test("updated_at is updated on update") {
    val image = imageGenerator.generate

    val f = for {
      inserted <- imageRepository.persist(image.namespace, image.commit, image.ref, image.description, image.pullUri)
      _ <- Future.successful(Thread.sleep(2000))
      updated <- imageRepository.persist(image.namespace, image.commit, image.ref, "Other description", image.pullUri)
    } yield (inserted, updated)

    val (inserted, updated) = f.futureValue

    inserted.createdAt shouldBe updated.createdAt

    withClue("inserted.updated_at should be before updated.updated_at") {
      inserted.updatedAt.isBefore(updated.updatedAt) shouldBe true
    }
  }
}
