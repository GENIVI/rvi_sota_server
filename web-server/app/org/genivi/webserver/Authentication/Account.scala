/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.webserver.Authentication

/**
 * Case class for Account
 *
 * @param email Account email
 * @param name Account name
 * @param role Account role
 */
case class Account(id: String, role: Role)
