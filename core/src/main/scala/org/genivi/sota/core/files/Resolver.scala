/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.files

import eu.timepit.refined.string.Uri
import eu.timepit.refined.{Refined, Predicate}

import scala.io.Source
import java.io.File
import java.nio.file.Paths

object Types {
  type Checksum = String
  type Resolver = (String => Either[String, (File, Checksum)])

  trait ValidExtension
  implicit val validExtension : Predicate[ValidExtension, String] = Predicate.instance(
    ext => ext.length >= 1 && ext.forall(c => c.isLetter || c.isDigit),
    ext => s"(${ext} isn't a valid file extension"
  )

  type FileExtension = String Refined ValidExtension
  type Path = String Refined Uri
}

class Resolver(_path: Types.Path, _packageExtension: Types.FileExtension, _checksumExtension: Types.FileExtension)
    extends Types.Resolver {
  val path = Paths.get(_path.get)
  val packageExtension = _packageExtension.get
  val checksumExtension = _checksumExtension.get

  def apply(packageIdentifier: String): Either[String, (File, Types.Checksum)] = for {
    packageFile <- packageFile(packageIdentifier).right
    checksumFile <- checksumFile(packageIdentifier).right
  } yield (packageFile, Source.fromFile(checksumFile).mkString)

  private def packageFile(packageIdentifier: String): Either[String, File] =
    resolve(s"$packageIdentifier.$packageExtension")

  private def checksumFile(packageIdentifier: String): Either[String, File] =
    resolve(s"$packageIdentifier.$packageExtension.$checksumExtension")

  private def resolve(filePath: String): Either[String, File] = path.resolve(filePath).toFile match {
    case file if file.exists() => Right(file)
    case file => Left(s"${file.getAbsolutePath} does not exist")
  }
}
