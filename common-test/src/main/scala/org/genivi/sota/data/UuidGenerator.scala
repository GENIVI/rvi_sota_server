package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import org.scalacheck.{Arbitrary, Gen}


trait UuidGenerator {

  val genUuid: Gen[Uuid] = for {
    uuid <- Gen.uuid
  } yield Uuid(Refined.unsafeApply(uuid.toString))

  implicit lazy val arbUuid: Arbitrary[Uuid] = Arbitrary(genUuid)

}

object UuidGenerator extends UuidGenerator
