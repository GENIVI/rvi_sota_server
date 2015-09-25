/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

case class DeserializationException(cause: Throwable) extends Throwable(cause)

