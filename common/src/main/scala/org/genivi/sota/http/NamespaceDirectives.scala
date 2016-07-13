package org.genivi.sota.http

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives
import com.advancedtelematic.jwt.JsonWebToken
import com.typesafe.config.{Config, ConfigFactory}
import org.genivi.sota.data.Namespace.Namespace
import org.slf4j.LoggerFactory

import scala.util.Try

object NamespaceDirectives {
  lazy val logger = LoggerFactory.getLogger(this.getClass)

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

  def fromConfig(): Directive1[Namespace] = {
    val config = ConfigFactory.load().getString("auth.protocol")

    config match {
      case "oauth.idtoken" =>
        logger.info("Using namespace from id token")
        AuthNamespaceDirectives.authNamespace[IdToken]
      case "oauth.accesstoken" =>
        logger.info("Using namespace from access token")
        AuthNamespaceDirectives.authNamespace[JsonWebToken]
      case _ =>
        logger.info("Using namespace from default conf extractor")
        defaultNamespaceExtractor
    }
  }
}
