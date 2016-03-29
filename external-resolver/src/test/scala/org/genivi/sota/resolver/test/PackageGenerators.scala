package org.genivi.sota.resolver.test

import org.genivi.sota.data.PackageIdGenerators
import org.genivi.sota.resolver.packages.Package
import org.scalacheck.{Arbitrary, Gen}

trait PackageGenerators extends PackageIdGenerators{

  val genPackage: Gen[Package] = for {
    id      <- genPackageId
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(id, desc, vendor)

  implicit val arbPackage: Arbitrary[Package] =
    Arbitrary(genPackage)

}

object PackageGenerators extends PackageGenerators
