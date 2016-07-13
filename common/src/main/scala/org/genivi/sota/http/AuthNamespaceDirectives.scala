package org.genivi.sota.http

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Directives, Rejection}
import cats.data.Xor
import com.advancedtelematic.jws.CompactSerialization
import com.advancedtelematic.jwt.{JsonWebToken, Subject}
import io.circe.parser._
import org.genivi.sota.data.Namespace._
import eu.timepit.refined.refineV
import io.circe.Decoder

/**
  * Type class defining an extraction of namespace information from a token of type `T`
  * @tparam T type of a token
  */
trait NsFromToken[T] {
  def namespace(token: T): Either[String, Namespace]
}

object NsFromToken {

  implicit val NsFromIdToken = new NsFromToken[IdToken] {
    override def namespace(token: IdToken): Either[String, Namespace] = refineV(token.sub.underlying)
  }

  implicit val NsFromJwt = new NsFromToken[JsonWebToken] {
    override def namespace(token: JsonWebToken): Either[String, Namespace] = refineV(token.subject.underlying)
  }

}

/**
  * Identity token
  * @param sub Subject claim
  */
final case class IdToken(sub: Subject)

object IdToken {

  import io.circe.generic.semiauto._
  import org.genivi.sota.marshalling.CirceInstances._
  implicit val DecoderInstance: Decoder[IdToken] = deriveDecoder[IdToken]

}

object AuthNamespaceDirectives {
  import Directives._

  private[this] def badNamespaceRejection(msg: String): Rejection = AuthorizationFailedRejection

  private[this] def extractToken[T: NsFromToken](serializedToken: String)
                                                     (implicit decoder: Decoder[T]): Xor[String, T] =
    for {
      serialized <- CompactSerialization.parse(serializedToken)
      token      <- decode[T](serialized.encodedPayload.stringData()).leftMap(_.getMessage)
    } yield token

  def authNamespace[T](implicit nsFromToken: NsFromToken[T], decoder: Decoder[T]): Directive1[Namespace] =
    extractCredentials flatMap { creds =>
      val maybeNamespace = creds match {
        case Some(OAuth2BearerToken(serializedToken)) =>
          for {
            token <- extractToken[T](serializedToken)
            nsE = nsFromToken.namespace(token)
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
