/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import org.genivi.sota.data.{Device, PackageId}

object ResolveFunctions {

  def makeFakeDependencyMap(pkgId: PackageId,
                            vs: Seq[Device.Id]): Map[Device.Id, List[PackageId]] =
    vs.map(device => Map(device -> List(pkgId)))
      .foldRight(Map.empty[Device.Id, List[PackageId]])(_++_)
}
