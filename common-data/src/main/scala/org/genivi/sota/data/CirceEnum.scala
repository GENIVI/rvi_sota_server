/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import io.circe.{Decoder, Encoder}

trait CirceEnum extends Enumeration {
  implicit val encode: Encoder[Value] = Encoder[String].contramap(_.toString)
  implicit val decode: Decoder[Value] = Decoder[String].map(this.withName)
}
