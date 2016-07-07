/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.http

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.server.{Directives, _}
import com.advancedtelematic.jwa.`HMAC SHA-256`
import com.advancedtelematic.jws.{CompactSerialization, JwsVerifier, KeyLookup}
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory

object AuthDirectives {
  import com.advancedtelematic.akka.http.jwt.JwtDirectives._

  lazy val logger = LoggerFactory.getLogger(this.getClass)

  type AuthScope = String

  def authPlusSignatureVerifier(secret: String): CompactSerialization => Boolean = {
    import com.advancedtelematic.json.signature.JcaSupport._
    val key: SecretKey = new SecretKeySpec(Base64.decodeBase64(secret), "HMAC")
    JwsVerifier().algorithmAndKeys(`HMAC SHA-256`, KeyLookup.const(key)).verifySignature _
  }

  def oauth(key: String): AuthScope => Directive0 = authScope => {
    // TODO: Use this instead of _ => true
    // It was using _ => in master so not changing this now
    val validation = authPlusSignatureVerifier(key)

    authenticateJwt("auth-plus", _ => true) flatMap { jwt =>
      oauth2Scope(jwt, authScope)
    }
  }

  val allowAll: AuthScope => Directive0 = _ => Directives.pass

  def fromConfig(): AuthScope => Directive0 = {
    val config = ConfigFactory.load()
    val protocol = config.getString("auth.protocol")
    lazy val authKey = config.getString("auth.token.secret")

    protocol match {
      case "oauth" =>
        logger.info("Using oauth authentication")
        oauth(authKey)
      case _ =>
        logger.info("Using `allowAll` for authentication")
        allowAll
    }
  }
}
