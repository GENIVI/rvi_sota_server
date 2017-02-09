/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}


trait GroupGenerators {

  private lazy val defaultNs: Namespace = Namespace("default")

  val genGroupName: Gen[Group.Name] = for {
    strLen <- Gen.choose(2, 100)
    name   <- Gen.listOfN[Char](strLen, Arbitrary.arbChar.arbitrary)
  } yield Refined.unsafeApply(name.mkString)

  def genGroupInfo: Gen[Group] = for {
    name      <- genGroupName
  } yield Group(Uuid.generate(), name, defaultNs)

  implicit lazy val arbGroupName: Arbitrary[Group.Name] = Arbitrary(genGroupName)
  implicit lazy val arbGroupInfo: Arbitrary[Group] = Arbitrary(genGroupInfo)
}

object GroupGenerators extends GroupGenerators
