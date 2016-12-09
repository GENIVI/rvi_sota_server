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

  val fromHeader: Directive1[Option[Namespace]] =
    optionalHeaderValueByName(NAMESPACE).map(_.map(Namespace(_)))

  lazy val defaultNamespaceExtractor: Directive1[AuthedNamespaceScope] = fromHeader.flatMap {
    case Some(ns) => provide(AuthedNamespaceScope(ns))
    case None => provide(AuthedNamespaceScope(configNamespace(ConfigFactory.load())))
  }

  def fromConfig(): Directive1[AuthedNamespaceScope] =
    ConfigFactory.load().getString("auth.protocol") match {
      case "oauth.idtoken" =>
        logger.info("Using namespace from id token")
        fromHeader.flatMap(AuthNamespaceDirectives.authNamespace[IdToken])
      case "oauth.accesstoken" =>
        logger.info("Using namespace from access token")
        fromHeader.flatMap(AuthNamespaceDirectives.authNamespace[JsonWebToken])
      case _ =>
        logger.info("Using namespace from default conf extractor")
        defaultNamespaceExtractor
    }
}
