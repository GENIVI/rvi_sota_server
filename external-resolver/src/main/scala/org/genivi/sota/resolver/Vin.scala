package org.genivi.sota.resolver

import spray.json.DefaultJsonProtocol


case class Vin(vin: String)

trait Protocols extends DefaultJsonProtocol {
  implicit val vinFormat = jsonFormat1(Vin.apply)
}
