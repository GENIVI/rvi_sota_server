/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.data

import org.genivi.sota.data.{Namespace, PackageId, Uuid}

case class AutoInstall(namespace: Namespace,
                       pkgName: PackageId.Name,
                       device: Uuid)
