/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.Authentication

sealed trait Role

case object User extends Role
