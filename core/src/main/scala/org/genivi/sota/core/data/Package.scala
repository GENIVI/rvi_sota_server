/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

case class Package(
  id: Option[Long],
  name: String,
  version: String,
  description: Option[String],
  vendor: Option[String]
) {
  def fullName: String = s"$name-$version"
}

object Package {
  import spray.json.DefaultJsonProtocol._
  implicit val pkgFormat = jsonFormat5(Package.apply)
}
