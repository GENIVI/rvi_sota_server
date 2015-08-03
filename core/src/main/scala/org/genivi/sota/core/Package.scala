/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

case class Package(
  id: Option[Long],
  name: String,
  version: String,
  description: Option[String],
  vendor: Option[String]
) {
  override def toString: String = s"$name--$version"
}
