package org.genivi.sota.resolver.test

import org.genivi.sota.data.{InvalidPackageIdGenerators, Namespaces, PackageIdGenerators}
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

  def getPackage: Package = genPackage.sample.getOrElse(getPackage)

}

object PackageGenerators extends PackageGenerators

/**
  * Generators for invalid data are kept in dedicated scopes
  * to rule out their use as implicits (impersonating valid ones).
  */
trait InvalidPackageGenerators extends InvalidPackageIdGenerators with Namespaces {

  val genInvalidPackage: Gen[Package] = for {
    id      <- genInvalidPackageId
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(defaultNs, id, desc, vendor)

  def getInvalidPackage: Package = genInvalidPackage.sample.getOrElse(getInvalidPackage)

}

object InvalidPackageGenerators extends InvalidPackageGenerators
