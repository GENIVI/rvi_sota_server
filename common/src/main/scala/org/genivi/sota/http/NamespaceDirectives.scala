package org.genivi.sota.http

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive1
import com.advancedtelematic.jwt.JsonWebToken
import com.typesafe.config.{Config, ConfigFactory}
import org.genivi.sota.data.Namespace
import org.slf4j.LoggerFactory

import scala.util.Try

object NamespaceDirectives {
  import akka.http.scaladsl.server.Directives._

  lazy val logger = LoggerFactory.getLogger(this.getClass)

  val NAMESPACE = "x-ats-namespace"

  def nsHeader(ns: Namespace): HttpHeader = RawHeader(NAMESPACE, ns.get)

  def configNamespace(config: Config): Namespace = {
    Namespace( Try(config.getString("core.defaultNs")).getOrElse("default"))
  }

  lazy val defaultNamespaceExtractor: Directive1[Namespace] = {
    extractRequest.flatMap { req =>
      req.headers.find(_.is(NAMESPACE)).map(_.value()) match {
        case Some(ns) => provide(Namespace(ns))
        case None => provide(configNamespace(ConfigFactory.load()))
      }
    }
  }

  def fromConfig(): Directive1[Namespace] = {
    ConfigFactory.load().getString("auth.protocol") match {
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
