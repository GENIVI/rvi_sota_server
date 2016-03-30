/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.Uri
import eu.timepit.refined.api.Refined
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.security.MessageDigest
import java.util.UUID
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.data._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{Namespaces, PackageId, Vehicle, VehicleGenerators}
import org.scalacheck.{Arbitrary, Gen}


/**
 * Generators for property-based testing of core objects
 */
trait Generators {

  import Namespaces._

  val PackageVersionGen: Gen[PackageId.Version] =
    Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined.unsafeApply)

  val PackageNameGen: Gen[PackageId.Name] =
    Gen.identifier.map(s => if (s.length > 100) s.substring(0, 100) else s).map(Refined.unsafeApply)

  val PackageIdGen = for {
    name    <- PackageNameGen
    version <- PackageVersionGen
  } yield PackageId( name, version )

  val PackageGen: Gen[Package] = for {
    id      <- PackageIdGen
    size    <- Gen.choose(1000L, 999999999L)
    cs      <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaChar)
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(defaultNs, id, Uri(path = Uri.Path / "tmp" / s"${id.name.get}-${id.version.get}.rpm"),
                  size, cs.mkString, desc, vendor, None)

  implicit val arbitrayPackage: Arbitrary[Package] = Arbitrary( PackageGen )

  import com.github.nscala_time.time.Imports._

  def updateRequestGen(namespaceGen: Gen[Namespace], packageIdGen : Gen[PackageId]) : Gen[UpdateRequest] = for {
    ns           <- namespaceGen
    packageId    <- packageIdGen
    startAfter   <- Gen.choose(10, 100).map( DateTime.now + _.days)
    finishBefore <- Gen.choose(10, 100).map(x => startAfter + x.days)
    prio         <- Gen.choose(1, 10)
    sig          <- Gen.alphaStr
    desc         <- Gen.option(Arbitrary.arbitrary[String])
    reqConfirm   <- Arbitrary.arbitrary[Boolean]
  } yield UpdateRequest(UUID.randomUUID(), ns, packageId, DateTime.now, startAfter to finishBefore,
                        prio, sig, desc, reqConfirm)

  def vinDepGen(packages: Seq[Package]) : Gen[(Vehicle.Vin, Set[PackageId])] = for {
    vin               <- VehicleGenerators.genVin
    m                 <- Gen.choose(1, 10)
    packages          <- Gen.pick(m, packages).map( _.map(_.id) )
  } yield vin -> packages.toSet

  def dependenciesGen(packages: Seq[Package] ) : Gen[UpdateService.VinsToPackages] = for {
    n <- Gen.choose(1, 10)
    r <- Gen.listOfN(n, vinDepGen(packages))
  } yield r.toMap

  def generatePackageData( template: Package ) : Package = {
    val path = Files.createTempFile(s"${template.id.name.get}-${template.id.version.get}", ".rpm" )
    path.toFile().deleteOnExit();
    val in = Files.newByteChannel(Paths.get("/dev/urandom"), StandardOpenOption.READ)
    val out = Files.newByteChannel(path, StandardOpenOption.WRITE)

    val buffer = java.nio.ByteBuffer.allocate(4096)
    var toRead : Int = template.size.intValue()
    val digest = MessageDigest.getInstance("SHA-1")

    while( toRead > 0 ) {
      buffer.clear()
      if( toRead < 4096 ) buffer.limit(toRead)
      val read = in.read(buffer)
      toRead -= read
      buffer.flip()
      out.write(buffer)
      buffer.flip()
      digest.update(buffer)
    }

    in.close()
    out.close()

    template.copy( uri = Uri( path.toUri().toString() ), checkSum = Hex.encodeHexString( digest.digest() ))
  }

  val updateSpecGen: Gen[(Package, Vehicle, UpdateSpec)] = for {
    smallSize <- Gen.chooseNum(1024, 1024 * 10)
    packageModel <- PackageGen.map(_.copy(size = smallSize.toLong))
    packageWithUri = Generators.generatePackageData(packageModel)
    vehicle <- VehicleGenerators.genVehicle
    updateRequest <- updateRequestGen(defaultNs, PackageIdGen).map(_.copy(packageId = packageWithUri.id))
  } yield {
    val updateSpec = UpdateSpec(defaultNs, updateRequest, vehicle.vin,
      UpdateStatus.Pending, List(packageWithUri).toSet)

    (packageWithUri, vehicle, updateSpec)
  }
}

object Generators extends Generators
