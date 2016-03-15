/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import org.scalacheck.{Arbitrary, Gen}

trait PackageIdGenerators {

  /**
    * For property based testing purposes, we need to explain how to
    * randomly generate package ids.
    *
    * @see [[https://www.scalacheck.org/]]
    */

  val genPackageId: Gen[PackageId] =
    for {
      name    <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar).map(cs => Refined.unsafeApply(cs.mkString))
        : Gen[PackageId.Name]
      version <- Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString("."))
                   .map(Refined.unsafeApply): Gen[PackageId.Version]
    } yield PackageId(name, version)

  implicit lazy val arbPackageId: Arbitrary[PackageId] =
    Arbitrary(genPackageId)

}

object PackageIdGenerators extends PackageIdGenerators
