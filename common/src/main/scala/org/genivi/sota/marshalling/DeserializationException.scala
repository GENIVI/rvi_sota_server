/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

/**
 * Unmarshalling into JSON sometimes fails, see CirceMarshallingSupport.scala.
 */

case class DeserializationException(cause: Throwable) extends Throwable(cause)
