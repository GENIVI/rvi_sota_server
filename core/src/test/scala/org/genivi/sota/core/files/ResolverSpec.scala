/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.files

import java.io.{FileOutputStream, File}
import java.nio.file.{Path, Files, Paths}
import eu.timepit.refined._
import eu.timepit.refined.string.Uri

import org.genivi.sota.core.files.Types.ValidExtension
import org.scalatest._

/**
 * WordSpec tests for resolver
 */
class ResolverSpec extends WordSpec with Matchers {
  def createTestDir: Path = Files.createTempDirectory(Paths.get("/tmp"), "packages")

  def createFile(dir: Path, packageIdentifier: String, extension: String, contents: String): File = {
    val file = Files.createFile(dir.resolve(packageIdentifier + s".$extension")).toFile
    val fileOutFile: FileOutputStream = new FileOutputStream(file)
    fileOutFile.write(contents.toCharArray.map(_.toByte))
    fileOutFile.close()
    file
  }

  def createResolver(packagesDir: Path, fileExtension: String = "rpm", checksumExtension: String = "sha1"): Resolver =
    (for {
      pkgDir <- refineV[Uri](packagesDir.toFile.getAbsolutePath).right
      fileExt <- refineV[ValidExtension](fileExtension).right
      chkExt <- refineV[ValidExtension](checksumExtension).right
    } yield new Resolver(pkgDir, fileExt, chkExt)).fold(err => fail(err), identity)

  def createPackage(dir: Path, packageIdentifier: String, contents: String = "foobar"): File =
    createFile(dir, packageIdentifier, "rpm", contents)

  def createChecksum(dir: Path, packageIdentifier: String, contents: String = "MYCHECKSUM"): Types.Checksum = {
    createFile(dir, packageIdentifier, "rpm.sha1", contents)
    contents
  }

  "A resolver returns a package with its checksum when both exist" in {
    val packagesDir = createTestDir
    val resolve = createResolver(packagesDir)
    val expectedFile = createPackage(packagesDir, "vim-2.1")
    val expectedChecksum = createChecksum(packagesDir, "vim-2.1")

    resolve("vim-2.1") match {
      case Right((file, checksum)) => {
        file.getAbsolutePath should equal(expectedFile.getAbsolutePath)
        checksum should equal(expectedChecksum)
      }
      case Left(e) => fail(e)
    }
  }

  "A resolver errors when the package does not exist at all" in {
    val packagesDir = createTestDir
    val resolve = createResolver(packagesDir)

    resolve("vim-2.1") match {
      case Right((file, checksum)) => fail("Found a package")
      case Left(e) => e should include ("vim-2.1.rpm does not exist")
    }
  }

  "A resolver errors when the package exists without a checksum" in {
    val packagesDir = createTestDir
    val resolve = createResolver(packagesDir)

    createPackage(packagesDir, "vim-2.1")

    resolve("vim-2.1") match {
      case Right((file, checksum)) => fail("Found a package with a checksum")
      case Left(e) => e should include ("vim-2.1.rpm.sha1 does not exist")
    }
  }
}
