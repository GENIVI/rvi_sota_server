package org.genivi.sota.resolver.types

case class Vin(vin: String)

object Vin {
  import eu.timepit.refined._
  import org.genivi.sota.resolver.Validation._
  import shapeless.tag.@@
  import spray.json.DefaultJsonProtocol._

  implicit val vinFormat = jsonFormat1(Vin.apply)

  type ValidVin = Vin @@ Valid

  implicit val validVin: Predicate[Valid, Vin] =
    Predicate.instance(vin => vin.vin.length == 17 && vin.vin.forall(c => c.isLetter || c.isDigit),
                       vin => s"(${vin} isn't 17 letters or digits long)")
}
