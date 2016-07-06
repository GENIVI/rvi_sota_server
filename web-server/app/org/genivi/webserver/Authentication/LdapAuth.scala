/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.webserver.Authentication

import akka.NotUsed
import akka.actor.ActorSystem
import com.typesafe.sslconfig.akka.util.AkkaLoggerFactory
import com.unboundid.ldap.sdk._
import javax.inject.{Inject, Singleton}
import javax.net.SocketFactory
import javax.net.ssl.SSLContext

import com.typesafe.sslconfig.ssl._
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

final case class AuthenticationConfig(bind: (String, String) => BindRequest, userDn: String, password: String) {
  def toBindRequest: BindRequest = bind(userDn, password)
}

final case class ConnectionConfig(host: String, port: Int, poolSize: Int, socketFactory: SocketFactory,
                                  bindRequest: Option[BindRequest])

final case class SearchFailed(baseDN: String, filter: String, entryCount: Int)
    extends Throwable(s"Failed to find unique entry: baseDN='$baseDN', filter='$filter'. Result size: $entryCount")

final case class AuthenticationFailed(id: String, cause: Option[Throwable])
    extends Throwable(s"Authentication of entry '$id' failed", cause.orNull)

@Singleton
class LdapAuth @Inject() (config: Configuration, lifecicle: ApplicationLifecycle, akka: ActorSystem) {

  val logger: Logger = Logger(this.getClass())

  val DefaultPoolSize = 10

  def bindRequestBuilder(authnType: String): (String, String) => BindRequest = authnType match {
    case "digest_md5" => (un, pswd) => new DIGESTMD5BindRequest(un, pswd)
    case "simple" => (un, pswd) => new SimpleBindRequest(un, pswd)
    case "plain" => (un, pswd) => new PLAINBindRequest(un, pswd)
    case "cram_md5" => (un, pswd) => new CRAMMD5BindRequest(un, pswd)
  }

  implicit val executionContext = akka.dispatcher

  val authenticationConfig: Option[AuthenticationConfig] = for {
    c <- config.getConfig("ldap.connection.authentication")
    b <- c.getString("type", Some(Set("simple", "plain", "cram_md5", "digest_md5"))).map(bindRequestBuilder)
    u <- c.getString("dn")
    p <- c.getString("password")
  } yield AuthenticationConfig(b, u, p)

  def ldapSocketFactory(): SocketFactory = {
    config.getConfig("ldap.connection.ssl").map{ c =>
      val sslConfig = SSLConfigFactory.parse(
        c.underlying.withFallback(config.underlying.getConfig("default.ssl-config")) )
      val logFactory = new AkkaLoggerFactory(akka)
      val sslContext = if (sslConfig.default) {
        logger.info("buildSSLContext: ws.ssl.default is true, using default SSLContext")
        SSLContext.getDefault
      } else {
        // break out the static methods as much as we can...
        val keyManagerFactory = buildKeyManagerFactory(sslConfig)
        val trustManagerFactory = buildTrustManagerFactory(sslConfig)
        new ConfigSSLContextBuilder(logFactory, sslConfig, keyManagerFactory, trustManagerFactory).build()
      }
      sslContext.getSocketFactory
    }.getOrElse(SocketFactory.getDefault)
  }

  private[this] def buildKeyManagerFactory(ssl: SSLConfigSettings): KeyManagerFactoryWrapper = {
    new DefaultKeyManagerFactoryWrapper(ssl.keyManagerConfig.algorithm)
  }

  private[this] def buildTrustManagerFactory(ssl: SSLConfigSettings): TrustManagerFactoryWrapper = {
    new DefaultTrustManagerFactoryWrapper(ssl.trustManagerConfig.algorithm)
  }

  val connectionConfig = (
      for {
        c <- config.getConfig("ldap.connection")
        host <- c.getString("host")
        port <- c.getInt("port")
      } yield ConnectionConfig(host, port,
        c.getInt("poolSize").getOrElse(DefaultPoolSize),
        ldapSocketFactory(),
        authenticationConfig.map(_.toBindRequest))
  ).get

  private[this] def connect(): LDAPConnectionPool =
    connectionConfig.bindRequest.map( br =>
      new LDAPConnectionPool( new SingleServerSet(connectionConfig.host, connectionConfig.port),
        br,
        connectionConfig.poolSize)
    ).getOrElse(
      new LDAPConnectionPool(
        new LDAPConnection(connectionConfig.host, connectionConfig.port), connectionConfig.poolSize)
    )

  val ldap: LDAPConnectionPool = connect() // let the application crash if misconfigured
  lifecicle.addStopHook{() => Future.successful( ldap.close()) }

  val authenticate: (String, String) => Future[Account] = ( for {
    c      <- config.getConfig("ldap.search")
    baseDN <- c.getString("baseDN")
    filter <- c.getString("searchFilter")
  } yield bindAuthN(baseDN, filter) _ ).get


  private[this] def findUser(baseDN: String, filter: String, userName: String): Future[SearchResult] =
    Future {
      ldap.search(baseDN, SearchScope.ONE, filter.format(userName))
    }

  private[this] def bind(dn: String, password: String): Future[NotUsed] = Future {
    val connection = new LDAPConnection(connectionConfig.host, connectionConfig.port)
    try {
      connection.bind(
        authenticationConfig.map(_.bind(dn, password) ).getOrElse(new SimpleBindRequest(dn, password)) )
    } finally { connection.close() }
    NotUsed
  }.recoverWith {
    case t: LDAPBindException => Future.failed[NotUsed]( AuthenticationFailed(dn, Some(t)) )
  }

  /**
    * This authentication method first searches for a user in the directory, and if it is found tries to
    * bind using returned DN and the password passed as function parameter.
    */
  private[this] def bindAuthN(baseDN: String, filter: String)(userName: String, password: String): Future[Account] =
    for {
      sr <- findUser(baseDN, filter, userName)
      dn <- if( sr.getEntryCount == 1) {
              Future.successful(sr.getSearchEntries.get(0).getDN)
            } else {
              Future.failed( SearchFailed(baseDN, filter.format(userName), sr.getEntryCount) )
            }
      _  <- bind(dn, password)
    } yield Account(dn, User)

}

