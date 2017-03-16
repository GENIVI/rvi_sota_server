/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.common

import akka.http.scaladsl.server.{Directives, Directive1}
import org.genivi.sota.data.PackageId
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.Validation._
import Directives._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._

/**
  * Helpers for extracting refined values from the URL.
  */

object RefinementDirectives {

  val refinedFilterName: Directive1[Filter.Name] =
    refined[Filter.ValidName](Slash ~ Segment)

  val refinedPackageId: Directive1[PackageId] =
    (refined[PackageId.ValidName](Slash ~ Segment) &
     refined[PackageId.ValidVersion](Slash ~ Segment))
       .as[PackageId](PackageId.apply _)

  val refinedPartNumber: Directive1[Component.PartNumber] =
    refined[Component.ValidPartNumber](Slash ~ Segment)

  val refinedPackageIdParams: Directive1[PackageId] = {
    parameters(('package_name.as[PackageId.Name], 'package_version.as[PackageId.Version]))
      .tmap { case (name, version) => PackageId(name, version) }
  }
}
