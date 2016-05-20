/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.common

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives
import com.typesafe.config.Config
import eu.timepit.refined._
import eu.timepit.refined.string._
import org.genivi.sota.data.Namespace._

import scala.util.Try


trait NamespaceDirective extends BasicDirectives {

  type NamespaceE = Either[String, Namespace]

  def configNamespace(config: Config): Option[Namespace] = {
    val namespaceString = Try(config.getString("core.defaultNs")).getOrElse("default")
    val nsE: NamespaceE = refineV(namespaceString)
    nsE.right.toOption
  }

  def defaultNs(implicit system: ActorSystem): Option[Namespace] = {
    configNamespace(system.settings.config)
  }

  import eu.timepit.refined.auto._

  def extractNamespace(implicit system: ActorSystem): Directive1[Namespace] = extract { _ =>
    defaultNs.getOrElse("")
  }
}

object NamespaceDirective extends NamespaceDirective
