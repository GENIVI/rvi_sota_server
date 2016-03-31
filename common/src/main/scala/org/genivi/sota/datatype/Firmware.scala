/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import eu.timepit.refined.api.{Refined, Validate}

trait FirmwareCommon {

  case class ValidName()
  case class ValidVersion()

  type Module = Refined[String, ValidName]
  type FirmwareId = Refined[String, ValidVersion]

  implicit val validModuleName: Validate.Plain[String, ValidName] =
    Validate.fromPredicate(
      s => s.length > 0 && s.length <= 100
        && s.forall(c => c.isLetter || c.isDigit),
      s => s"$s: isn't a valid module name (between 1 and 100 character long alpha numeric string)",
      ValidName()
    )

  implicit val validFirmwareId: Validate.Plain[String, ValidVersion] =
    Validate.fromPredicate(
      _.matches( """.+""" ),
      _ => "Invalid version id format",
      ValidVersion()
    )

}
