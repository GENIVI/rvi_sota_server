/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.data

object UpdateType extends CirceEnum with SlickEnum {
  type UpdateType = Value

  val Image, Package = Value
}
