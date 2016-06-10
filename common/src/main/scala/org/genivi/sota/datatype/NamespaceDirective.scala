/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives
import com.typesafe.config.{Config, ConfigFactory}
import eu.timepit.refined._
import eu.timepit.refined.string._
import org.genivi.sota.data.Namespace._

import scala.util.Try

object NamespaceDirective {
  import eu.timepit.refined.refineV

  def configNamespace(config: Config): Option[Namespace] = {
    val namespaceString = Try(config.getString("core.defaultNs")).getOrElse("default")
    val nsE: Either[String, Namespace] = refineV(namespaceString)
    nsE.right.toOption
  }

  private lazy val defaultConfigNamespace: Namespace = {
    configNamespace(ConfigFactory.load()) getOrElse {
      val nsE: Either[String, Namespace] = refineV("default-config-ns")
      nsE.right.toOption.get
    }
  }

  lazy val defaultNamespaceExtractor: Directive1[Namespace] =
    BasicDirectives.provide(defaultConfigNamespace)
}
