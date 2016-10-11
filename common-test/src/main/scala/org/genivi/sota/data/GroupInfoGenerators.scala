/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}


trait GroupInfoGenerators {

  private lazy val defaultNs: Namespace = Namespace("default")

  val genGroupName: Gen[GroupInfo.Name] = for {
    strLen <- Gen.choose(2, 100)
    name   <- Gen.listOfN[Char](strLen, Arbitrary.arbChar.arbitrary)
  } yield Refined.unsafeApply(name.mkString)

  def genDiscardedAttrs(d: Int = 5): Gen[Json] =
    if (d > 0) Gen.oneOf(Gen.const(Json.Null), genDiscardedObj(d-1))
    else       Gen.const(Json.Null)

  def genDiscardedObj(d: Int): Gen[Json] = for {
    ks <- Gen.resize(d, Gen.listOf(Gen.identifier))
    vs <- Gen.resize(d, Gen.listOf(genDiscardedAttrs(d)))
  } yield Json.fromFields(ks.zip(vs))

  def genGroupInfo: Gen[GroupInfo] = for {
    name      <- genGroupName
    groupInfo <- SimpleJsonGenerator.simpleJsonGen
    discarded <- genDiscardedAttrs()
  } yield GroupInfo(Uuid.generate(), name, defaultNs, groupInfo, discarded)

  implicit lazy val arbGroupName: Arbitrary[GroupInfo.Name] = Arbitrary(genGroupName)
  implicit lazy val arbGroupInfo: Arbitrary[GroupInfo] = Arbitrary(genGroupInfo)
}

object GroupInfoGenerators extends GroupInfoGenerators
