/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry.common

import slick.jdbc.MySQLProfile.api._
import io.circe.Json
import io.circe.jawn._

trait SlickJsonHelper {
  implicit val jsonColumnType = MappedColumnType.base[Json, String](
    {json => json.noSpaces},
    {str  => parse(str).fold(_ => Json.Null, x => x)}
  )
}
