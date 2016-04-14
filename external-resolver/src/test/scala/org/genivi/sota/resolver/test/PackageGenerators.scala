package org.genivi.sota.resolver.test

import org.genivi.sota.data.{Namespaces, PackageIdGenerators}
import org.genivi.sota.resolver.packages.Package
import org.scalacheck.{Arbitrary, Gen}

trait PackageGenerators extends PackageIdGenerators with Namespaces {

  val genPackage: Gen[Package] = for {
    id      <- genPackageId
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(defaultNs, id, desc, vendor)

  implicit val arbPackage: Arbitrary[Package] =
    Arbitrary(genPackage)

}

object PackageGenerators extends PackageGenerators
