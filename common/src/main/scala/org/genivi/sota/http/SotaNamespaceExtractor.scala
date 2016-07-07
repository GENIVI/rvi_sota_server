package org.genivi.sota.http

import akka.http.scaladsl.server.Directive1

import com.typesafe.config.ConfigFactory
import org.genivi.sota.data.Namespace.Namespace
import org.slf4j.LoggerFactory

object SotaNamespaceExtractor {
  lazy val logger = LoggerFactory.getLogger(this.getClass)

  def fromConfig(): Directive1[Namespace] = {
    val config = ConfigFactory.load().getString("auth.protocol")

    config match {
      case "oauth" =>
        logger.info("Using namespace from oauth")
        AuthNamespace.authNamespace
      case _ =>
        logger.info("Using namespace from default conf extractor")
        NamespaceDirective.defaultNamespaceExtractor
    }
  }
}
