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
import org.genivi.sota.data.Namespace

/**
  * Type class defining an extraction of namespace information from a token of type `T`
  * @tparam T type of a token
  */
trait NsFromToken[T] {
  def namespace(token: T): String
}

object NsFromToken {

  implicit val NsFromIdToken = new NsFromToken[IdToken] {
    override def namespace(token: IdToken): String = token.sub.underlying
  }

  implicit val NsFromJwt = new NsFromToken[JsonWebToken] {
    override def namespace(token: JsonWebToken): String = token.subject.underlying
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
          extractToken[T](serializedToken).map( nsFromToken.namespace )

        case _ => Xor.Left("No oauth token provided to extract namespace")
    }

    maybeNamespace match {
      case Xor.Right(t) => provide(Namespace(t))
      case Xor.Left(msg) =>
        extractLog flatMap { l =>
          l.info(s"Could not extract namespace: $msg")
          reject(badNamespaceRejection(msg))
        }
    }
  }
}
