/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import org.genivi.sota.data.{Device, PackageId, Uuid}

object ResolveFunctions {

  def makeFakeDependencyMap(pkgId: PackageId,
                            vs: Seq[Uuid]): Map[Uuid, List[PackageId]] =
    vs.map(device => Map(device -> List(pkgId)))
      .foldRight(Map.empty[Uuid, List[PackageId]])(_++_)
}
