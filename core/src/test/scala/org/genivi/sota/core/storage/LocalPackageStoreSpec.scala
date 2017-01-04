/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.actor.ActorSystem
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.HttpEntity
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import akka.util.ByteString
import org.genivi.sota.DefaultPatience
import org.genivi.sota.data.PackageIdGenerators
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuiteLike, ShouldMatchers}

class LocalPackageStoreSpec extends TestKit(ActorSystem("LocalPackageStoreSpec"))
  with FunSuiteLike
  with ShouldMatchers
  with ScalaFutures
  with DefaultPatience {

  implicit val mat = ActorMaterializer()

  val entity = HttpEntity(ByteString("""Build no Utopia, Lydia, for the time
                                       |You fancy yet to be, nor count upon
                                       |Tomorrow. Today fulfils itself, and does not wait.
                                       |You are yourself your life.
                                       |Contrive no plan, for you are not to be.
                                       |Perhaps between the cup you drain
                                       |And the same replenished, Fate
                                       |Will interpose the void.""".stripMargin))

  val fileData = StrictForm.FileData(Some("filename.rpm"), entity)

  test("writes file to local storage") {
    val storage = new LocalPackageStore()
    val packageId = PackageIdGenerators.genPackageId.sample.get

    val f = storage.store(packageId, fileData.filename.get, fileData.entity.dataBytes)

    whenReady(f) { case (uri, byteSize, digest) =>
      uri.path.toString should endWith("filename.rpm")
      digest shouldBe "8107fd9e4318dd77110498ac70b1796fec11bf8c"
      byteSize shouldBe 282
    }
  }

  test("builds response from local storage") {
    import system.dispatcher

    val storage = new LocalPackageStore()
    val packageId = PackageIdGenerators.genPackageId.sample.get

    val f = for {
      (uri, _, _) <- storage.store(packageId, fileData.filename.get, fileData.entity.dataBytes)
      (_, entity) <- storage.retrieve(packageId, uri)
      contents <- entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield contents

    whenReady(f) { contents =>
      contents shouldNot be(empty)
      contents shouldBe fileData.entity.data
    }
  }
}
