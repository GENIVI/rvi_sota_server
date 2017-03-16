package org.genivi.sota.device_registry.common

import org.genivi.sota.data.PackageId

case class PackageStat(packageVersion: PackageId.Version, installedCount: Int)

object PackageStat {
  import io.circe.Encoder
  import io.circe.generic.semiauto._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  implicit val encoder: Encoder[PackageStat] = deriveEncoder[PackageStat]
}
