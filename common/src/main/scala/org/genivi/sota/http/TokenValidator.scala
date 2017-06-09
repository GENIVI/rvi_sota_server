/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpEntity,Multipart,StatusCodes,Uri}
import akka.http.scaladsl.model.headers.{Authorization,BasicHttpCredentials,OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0, Directives}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.either._
import com.advancedtelematic.akka.http.jwt.JwtDirectives
import com.advancedtelematic.json.signature.JcaSupport
import com.advancedtelematic.jwa.HS256
import com.advancedtelematic.jws.Jws
import com.typesafe.config.{ConfigException, ConfigFactory}
import io.circe.Json
import io.circe.generic.auto._
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure,Success}

case class ValidationResponse(active: Boolean)

object TokenValidator {
  def apply()(implicit system: ActorSystem, mat: ActorMaterializer) = new TokenValidator
}

class TokenValidator(implicit system: ActorSystem, mat: ActorMaterializer) {
  import CirceMarshallingSupport._
  import Directives._
  import Json._

  private val config = ConfigFactory.load()
  val logger = LoggerFactory.getLogger(this.getClass)

  val authProtocol = config.getString("auth.protocol")
  val shouldVerify = config.getString("auth.verification")
  lazy val authPlusUri = Uri(config.getString("authplus.api.uri"))
  lazy val clientId = config.getString("authplus.client.id")
  lazy val clientSecret = config.getString("authplus.client.secret")

  private def authPlusCheckToken(token: String)
                                (implicit ec: ExecutionContext): Future[Boolean] = {
    import StatusCodes._
    val uri = authPlusUri.withPath(Uri("/introspect").path)
    val entity = HttpEntity(`application/json`, fromString(token).noSpaces )
    val form = Multipart.FormData(Map("token" -> entity))
    val request = Post(uri,form) ~> Authorization(BasicHttpCredentials(clientId, clientSecret))

    for {
      response <- Http().singleRequest(request)
      status <- response.status match {
        case OK => Unmarshal(response.entity).to[ValidationResponse].map(_.active)
        case _  => {
          FastFuture.failed(new Throwable(s"auth-plus doesn't return OK: ${response.toString}"))
        }
      }
    } yield status
  }

  def authPlusValidate: Directive0 = extractCredentials.flatMap {
    case Some(OAuth2BearerToken(token)) =>
      extractExecutionContext.flatMap { implicit ec =>
        onComplete(authPlusCheckToken(token)) flatMap {
          case Success(true)  => {
            logger.info(s"Token was successfully verified via auth-plus")
            pass
          }
          case Success(false) => {
            logger.info("auth-plus rejects the token")
            reject(AuthorizationFailedRejection)
          }
          case Failure(err) => {
            logger.info(s"Couldn't connect with auth-plus (will try local validation): ${err.toString}")
            localValidate
          }
        }
      }
    case _ =>
      reject(AuthorizationFailedRejection)
  }

  def localValidate: Directive0 = {
    import JwtDirectives._
    import JcaSupport._

    val verifier: String Either Jws.JwsVerifier = for {
      secret <- Either
                 .catchOnly[ConfigException] {config.getString("auth.token.secret")}
                 .leftMap(_.getMessage)
                 .map[SecretKey](x => new SecretKeySpec(Base64.decodeBase64(x), "HMAC"))
      keyInfo <- HS256.verificationKey(secret).leftMap(_.getMessage)
    } yield HS256.verifier(keyInfo)

    verifier.fold (x => reject(AuthorizationFailedRejection),
                   x => authenticateJwt("auth-plus", x).flatMap{ _ =>
                     logger.info(s"Token was successfully verified locally")
                     pass})
  }

  def fromConfig(): Directive0 = {
    authProtocol match {
      case "none" => pass
      case _ => shouldVerify match {
        case "none"  => {
          logger.info("Will not verify tokens")
          pass
        }
        case "local" => {
          logger.info("Will verify tokens locally")
          localValidate
        }
        case "auth-plus" => {
          logger.info("Will verify tokens with auth-plus")
          authPlusValidate
        }
      }
    }
  }
}
