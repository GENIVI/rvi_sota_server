package org.genivi.sota.resolver.types

case class Package (
  id: Option[Long],
  name: String,
  version: String,
  description: Option[String],
  vendor: Option[String]
)

object Package {
  import eu.timepit.refined._
  import org.genivi.sota.resolver.Validation._
  import shapeless.tag.@@
  import spray.json.DefaultJsonProtocol._

  implicit val packageFormat = jsonFormat5(Package.apply)

  type ValidPackage = Package @@ Valid

  implicit val validPackage: Predicate[Valid, Package] =
    Predicate.instance(pkg => pkg.name.nonEmpty && pkg.version.nonEmpty,
                       pkg => s"(${pkg} has no name and/or version)")
}
