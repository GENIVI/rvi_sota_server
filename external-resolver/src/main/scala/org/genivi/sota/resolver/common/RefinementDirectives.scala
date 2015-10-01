/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.common

import akka.http.scaladsl.server.{Directives, Directive1}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.rest.Validation._


object RefinementDirectives {
  import Directives._

  def refinedPackageId: Directive1[Package.Id] =
    (refined[Package.ValidName]   (Slash ~ Segment) &
     refined[Package.ValidVersion](Slash ~ Segment ~ PathEnd))
       .as[Package.Id](Package.Id.apply _)

}
