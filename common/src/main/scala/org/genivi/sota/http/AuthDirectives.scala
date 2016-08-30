/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.http

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.server.{Directives, _}
import cats.data.Xor
import com.advancedtelematic.jwa.HS256
import com.advancedtelematic.jws.{CompactSerialization, Jws, KeyLookup}
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory

object AuthDirectives {
  import com.advancedtelematic.akka.http.jwt.JwtDirectives._

  lazy val logger = LoggerFactory.getLogger(this.getClass)

  type AuthScope = String

  def oauth(signVerifier: Jws.JwsVerifier): AuthScope => Directive0 = authScope => {
    authenticateJwt("auth-plus", signVerifier) flatMap { jwt =>
      oauth2Scope(jwt, authScope)
    }
  }

  val allowAll: AuthScope => Directive0 = _ => Directives.pass

  def fromConfig(): AuthScope => Directive0 = {
    val config   = ConfigFactory.load()
    val protocol = config.getString("auth.protocol")

    protocol match {
      case "none" =>
        logger.info("Using `allowAll` for authentication")
        allowAll
      case _ =>
        logger.info("Using oauth authentication")
        import com.advancedtelematic.json.signature.JcaSupport._
        val verifier: String Xor Jws.JwsVerifier = for {
          secret <- Xor
                     .catchOnly[ConfigException] { config.getString("auth.token.secret") }
                     .leftMap(_.getMessage)
                     .map[SecretKey](x => new SecretKeySpec(Base64.decodeBase64(x), "HMAC"))
          keyInfo <- HS256.verificationKey(secret).leftMap(_.getMessage)
        } yield HS256.verifier(keyInfo)

        oauth(verifier.fold(x => throw new Throwable(s"Unable to configure signature validation: $x"), identity))
    }
  }
}
