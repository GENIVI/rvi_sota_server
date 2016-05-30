/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.common

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives
import com.typesafe.config.{Config, ConfigFactory}
import eu.timepit.refined._
import eu.timepit.refined.string._
import org.genivi.sota.data.Namespace._

import scala.util.Try

object NamespaceDirective {
  def configNamespace(config: Config): Option[Namespace] = {
    val namespaceString = Try(config.getString("core.defaultNs")).getOrElse("default")
    val nsE: Either[String, Namespace] = refineV(namespaceString)
    nsE.right.toOption
  }

  lazy val defaultNs: Option[Namespace] = configNamespace(ConfigFactory.load())

  import eu.timepit.refined.auto._

  // TODO: Start here, remove this method, see what breaks
  lazy val defaultNamespace: Directive1[Namespace] =
    BasicDirectives.provide(defaultNs.getOrElse("default"))
}
