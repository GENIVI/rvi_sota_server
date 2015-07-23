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
