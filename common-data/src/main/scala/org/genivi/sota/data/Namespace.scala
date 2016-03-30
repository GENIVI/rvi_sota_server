/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri


object Namespace {
  // TODO schema for namespace; URI seems reasonable enough
  type Namespace = Refined[String, Uri]
}
