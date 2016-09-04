/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import slick.driver.MySQLDriver.MappedJdbcType
import slick.driver.MySQLDriver.api._

trait SlickEnum extends Enumeration {
  implicit val enumMapper = MappedJdbcType.base[Value, Int](_.id, this.apply)
}
