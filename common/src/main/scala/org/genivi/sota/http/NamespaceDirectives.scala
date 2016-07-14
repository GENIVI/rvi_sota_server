package org.genivi.sota.http

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives
import com.advancedtelematic.jwt.JsonWebToken
import com.typesafe.config.{Config, ConfigFactory}
import org.genivi.sota.data.Namespace
import org.slf4j.LoggerFactory

import scala.util.Try

object NamespaceDirectives {
  lazy val logger = LoggerFactory.getLogger(this.getClass)

  import eu.timepit.refined.refineV

  def configNamespace(config: Config): Namespace = {
    Namespace( Try(config.getString("core.defaultNs")).getOrElse("default"))
  }

  lazy val defaultNamespaceExtractor: Directive1[Namespace] =
    BasicDirectives.provide(configNamespace(ConfigFactory.load()))

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
