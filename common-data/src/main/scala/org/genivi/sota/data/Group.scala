/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import eu.timepit.refined.api.{Refined, Validate}

import Group._

case class Group(id: Uuid,
                 groupName: Name,
                 namespace: Namespace)

object Group {
  case class ValidName()

  type Name = Refined[String, ValidName]

  implicit val validGroupName: Validate.Plain[String, ValidName] =
    Validate.fromPredicate(
      name => name.length > 1 && name.length <= 100,
      name => s"($name should be between two and a hundred alphanumeric characters long.)",
      ValidName()
    )
}
