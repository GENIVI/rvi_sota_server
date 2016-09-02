/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import scala.util.control.NoStackTrace

/**
  * Sometimes validation (refinement) fails, see
  * RefinedMarshallingSupport.scala.
  */

case class RefinementError[T]( o: T, msg: String) extends Exception(msg) with NoStackTrace
