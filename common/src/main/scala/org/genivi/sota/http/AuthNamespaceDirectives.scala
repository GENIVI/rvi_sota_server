package org.genivi.sota.http

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Directives, Rejection}
import cats.data.Xor
import com.advancedtelematic.jws.CompactSerialization
import com.advancedtelematic.jwt.JsonWebToken
import io.circe.parser._
import org.genivi.sota.data.Namespace._
import eu.timepit.refined.refineV

object AuthNamespaceDirectives {
  import Directives._

  private[this] def badNamespaceRejection(msg: String): Rejection = AuthorizationFailedRejection

  private[this] def extractToken(serializedToken: String): Xor[String, JsonWebToken] =
    for {
      serialized <- CompactSerialization.parse(serializedToken)
      token      <- decode[JsonWebToken](serialized.encodedPayload.stringData()).leftMap(_.getMessage)
    } yield token

  lazy val authNamespace: Directive1[Namespace] = extractCredentials flatMap { creds =>
    val maybeNamespace = creds match {
      case Some(OAuth2BearerToken(serializedToken)) =>
        for {
          token <- extractToken(serializedToken)
          nsE = refineV(token.subject.underlying): Either[String, Namespace]
          ns <- Xor.fromEither(nsE)
        } yield ns
      case _ => Xor.Left("No oauth token provided to extract namespace")
    }

    maybeNamespace match {
      case Xor.Right(t) => provide(t)
      case Xor.Left(msg) =>
        extractLog flatMap { l =>
          l.info(s"Could not extract namespace: $msg")
          reject(badNamespaceRejection(msg))
        }
    }
  }
}
