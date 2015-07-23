package org.genivi.sota.resolver

case class Package (
  id: Option[Long],
  name: String,
  version: String,
  description: Option[String],
  vendor: Option[String]
)
