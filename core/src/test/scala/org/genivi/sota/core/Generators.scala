/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import eu.timepit.refined.api.Refined
import akka.http.scaladsl.model.Uri
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.data.{Vehicle, Package}, Vehicle._
import org.scalacheck.{Arbitrary, Gen}

/**
 * Generators for property-based testing of core objects
 */
trait Generators {

  val PackageVersionGen: Gen[Package.Version] =
    Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined.unsafeApply(_))

  val PackageNameGen: Gen[Package.Name] =
    Gen.identifier.map(Refined.unsafeApply(_))

  val PackageIdGen = for {
    name    <- PackageNameGen
    version <- PackageVersionGen
  } yield Package.Id( name, version )

  val PackageGen: Gen[Package] = for {
    id <- PackageIdGen
    size    <- Gen.choose(1000L, 999999999)
    cs      <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaChar)
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(id, Uri(path = Uri.Path / "tmp" / s"${id.name.get}-${id.version.get}.rpm"), size, cs.mkString, desc, vendor)

  implicit val arbitrayPackage: Arbitrary[Package] = Arbitrary( PackageGen )

  import com.github.nscala_time.time.Imports._

  def updateRequestGen(packageIdGen : Gen[Package.Id]) : Gen[UpdateRequest] = for {
    packageId    <- packageIdGen
    startAfter   <- Gen.choose(10, 100).map( DateTime.now + _.days)
    finishBefore <- Gen.choose(10, 100).map(x => startAfter + x.days)
    prio         <- Gen.choose(1, 10)
  } yield UpdateRequest( UUID.randomUUID(), packageId, DateTime.now, startAfter to finishBefore, prio )

  def vinDepGen(packages: Seq[Package]) : Gen[(Vehicle.Vin, Set[Package.Id])] = for {
    vin               <- genVin
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
}

object Generators extends Generators
