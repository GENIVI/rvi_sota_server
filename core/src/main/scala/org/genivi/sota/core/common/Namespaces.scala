/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.common

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives
import eu.timepit.refined._
import eu.timepit.refined.string._
import org.genivi.sota.data.Namespace._


trait NamespaceDirective extends BasicDirectives {

  type NamespaceE = Either[String, Namespace]

  def defaultNs(implicit system: ActorSystem): Option[Namespace] = {
    val nsE: NamespaceE = refineV(system.settings.config.getString("core.defaultNs"))
    nsE.right.toOption
  }

  import eu.timepit.refined.auto._

  def extractNamespace(implicit system: ActorSystem): Directive1[Namespace] = extract { _ =>
    defaultNs.getOrElse("")
  }
}

object NamespaceDirective extends NamespaceDirective
