package org.genivi.sota.resolver

import spray.json.{CollectionFormats, DefaultJsonProtocol}

trait Protocols extends DefaultJsonProtocol with CollectionFormats {
  implicit val vinFormat = jsonFormat1(Vin.apply)
  implicit val packageFormat = jsonFormat5(Package.apply)
  implicit val packageListFormat = seqFormat[Package]
}
