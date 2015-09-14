/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import akka.http.scaladsl.model.Uri
import cats.data.Xor
import eu.timepit.refined._


case class Package(
  id: Package.Id,
  uri: Uri,
  size: Long,
  checkSum: String,
  description: Option[String],
  vendor: Option[String]
)

object Package {

  case class Id(
    name: Package.Name,
    version: Package.Version
  )

  trait ValidName
  trait ValidVersion

  type Name    = String Refined ValidName
  type Version = String Refined ValidVersion

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format")

  implicit val packageIdShow = cats.Show.show[Package.Id]( packageId => s"${packageId.name}-${packageId.version}" )
}
