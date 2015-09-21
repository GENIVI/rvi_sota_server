/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import akka.http.scaladsl.model.Uri
import cats.Show
import cats.data.Xor
import eu.timepit.refined.{Refined, Predicate}


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

  object Id {
    implicit val showInstance : Show[Id] = Show.show( x => s"${x.name.get}-${x.version.get}" )
  }

  trait ValidName
  trait ValidVersion

  type Name    = String Refined ValidName
  type Version = String Refined ValidVersion

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+(\.\d+)?(-\d+)?$""" ), _ => "Invalid version format")

}
