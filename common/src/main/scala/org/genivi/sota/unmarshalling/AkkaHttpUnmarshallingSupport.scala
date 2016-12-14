/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.unmarshalling

import java.time.OffsetDateTime

import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}

trait AkkaHttpUnmarshallingSupport {
  implicit def offsetDateTimeUnmarshaller: FromStringUnmarshaller[OffsetDateTime] =
    Unmarshaller.strict(OffsetDateTime.parse)
}

object AkkaHttpUnmarshallingSupport extends AkkaHttpUnmarshallingSupport

