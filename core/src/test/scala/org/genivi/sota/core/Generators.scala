/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import eu.timepit.refined.Refined
import akka.http.scaladsl.model.Uri
import java.util.UUID
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.data.{Vehicle, Package}
import org.scalacheck.{Arbitrary, Gen}

trait Generators {

  val vehicleGen: Gen[Vehicle] = Gen.listOfN(17, Gen.alphaNumChar).map( xs => Vehicle( Refined(xs.mkString) ) )
  implicit val arbitraryVehicle : Arbitrary[Vehicle] = Arbitrary( vehicleGen )

  val PackageVersionGen: Gen[Package.Version] =
    Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined(_))

  val PackageNameGen: Gen[Package.Name] =
    Gen.identifier.map(Refined(_))

  val PackageIdGen = for {
    name    <- PackageNameGen
    version <- PackageVersionGen
  } yield Package.Id( name, version )

  val PackageGen: Gen[Package] = for {
    id <- PackageIdGen
    size    <- Gen.choose(1000L, 999999999)
    cs      <- Gen.alphaStr
    // This should be changed back to arbitrary strings once we
    // figured out where this encoding bug happens.
    desc    <- Gen.option(Gen.alphaStr)
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(id, Uri(path = Uri.Path / "tmp" / s"${id.name.get}-${id.version.get}.rpm"), size, cs, desc, vendor)

  implicit val arbitrayPackage: Arbitrary[Package] = Arbitrary( PackageGen )

  import com.github.nscala_time.time.Imports._

  def updateRequestGen(packageIdGen : Gen[Package.Id]) : Gen[UpdateRequest] = for {
    packageId    <- packageIdGen
    startAfter   <- Gen.choose(10, 100).map( DateTime.now + _.days)
    finishBefore <- Gen.choose(10, 100).map(x => startAfter + x.days)
    prio         <- Gen.choose(1, 10)
  } yield UpdateRequest( UUID.randomUUID(), packageId, DateTime.now, startAfter to finishBefore, prio )

  def vinDepGen(packages: Seq[Package]) : Gen[(Vehicle.IdentificationNumber, Set[Package.Id])] = for {
    vin               <- vehicleGen.map( _.vin )
    m                 <- Gen.choose(1, 10)
    packages          <- Gen.pick(m, packages).map( _.map(_.id) )
  } yield vin -> packages.toSet

  def dependenciesGen(packages: Seq[Package] ) : Gen[UpdateService.VinsToPackages] = for {
    n <- Gen.choose(1, 10)
    r <- Gen.listOfN(n, vinDepGen(packages))
  } yield r.toMap
}

object Generators extends Generators
