/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.resolver

trait VersionInfo {
  lazy val projectName: String = BuildInfo.name

  lazy val version: String = {
    val bi = BuildInfo
    s"${bi.name}/${bi.version}"
  }

  lazy val versionMap: Map[String, Any] = BuildInfo.toMap
}

