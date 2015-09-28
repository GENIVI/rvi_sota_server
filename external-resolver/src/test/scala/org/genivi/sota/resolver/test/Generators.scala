package org.genivi.sota.resolver.test

import eu.timepit.refined.Refined
import org.genivi.sota.resolver.packages.Package
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

object Generators {

  val genVersion: Gen[Package.Version] =
    Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined(_))

  val genPackageName: Gen[Package.Name] =
    Gen.identifier.map(Refined(_))

  val genPackage: Gen[Package] = for {
    name    <- genPackageName
    version <- genVersion
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(Package.Id(name, version), desc, vendor)

  implicit lazy val arbPackage = Arbitrary(genPackage)

}
