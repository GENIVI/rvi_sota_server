package org.genivi.sota.http

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0, Directive1, Directives, Rejection}
import akka.http.scaladsl.server.Directives._
import cats.syntax.either._
import com.advancedtelematic.akka.http.jwt.InvalidScopeRejection
import com.advancedtelematic.jws.CompactSerialization
import com.advancedtelematic.jwt.{JsonWebToken, Scope, Subject}
import io.circe.parser._
import io.circe.Decoder
import org.genivi.sota.data.Namespace

case class AuthedNamespaceScope(namespace: Namespace, scope: Option[Scope] = None) {
  type ScopeItem = String

  def hasScope(sc: ScopeItem) : Boolean = scope.isEmpty || scope.get.underlying.contains(sc)

  def hasScopeReadonly(sc: ScopeItem) : Boolean = hasScope(sc) || hasScope(sc + ".readonly")

  def oauthScope(scope: ScopeItem): Directive0 = {
    if (hasScope(scope)) pass
    else reject(InvalidScopeRejection(scope), AuthorizationFailedRejection)
  }

  def oauthScopeReadonly(scope: ScopeItem): Directive0 = {
    if (hasScopeReadonly(scope)) pass
    else reject(InvalidScopeRejection(scope), AuthorizationFailedRejection)
  }
}

object AuthedNamespaceScope {
  import scala.language.implicitConversions
  implicit def toNamespace(ns: AuthedNamespaceScope): Namespace = ns.namespace

  val namespacePrefix = "namespace."

  def apply(token: IdToken) : AuthedNamespaceScope = {
    AuthedNamespaceScope(Namespace(token.sub.underlying))
  }

  def apply(token: JsonWebToken) : AuthedNamespaceScope = {
    val nsSet = token.scope.underlying.collect {
      case x if x.startsWith(namespacePrefix) => x.substring(namespacePrefix.length)
    }
    if (nsSet.size == 1) {
      AuthedNamespaceScope(Namespace(nsSet.toVector(0)), Some(token.scope))
    } else {
      AuthedNamespaceScope(Namespace(token.subject.underlying), Some(token.scope))
    }
  }
}

/**
  * Type class defining an extraction of namespace information from a token of type `T`
  * @tparam T type of a token
  */
trait NsFromToken[T] {
  def toNamespaceScope(token: T): AuthedNamespaceScope
}

object NsFromToken {

  implicit val NsFromIdToken = new NsFromToken[IdToken] {
    override def toNamespaceScope(token: IdToken) = AuthedNamespaceScope(token)
  }

  implicit val NsFromJwt = new NsFromToken[JsonWebToken] {
    override def toNamespaceScope(token: JsonWebToken) = AuthedNamespaceScope(token)
  }

  def parseToken[T: NsFromToken](serializedToken: String)
    (implicit decoder: Decoder[T]): Either[String, T] =
    for {
      serialized <- CompactSerialization.parse(serializedToken)
      token      <- decode[T](serialized.encodedPayload.stringData()).leftMap(_.getMessage)
    } yield token

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

  def authNamespace[T](ns0: Option[Namespace])
                   (implicit nsFromToken: NsFromToken[T], decoder: Decoder[T]): Directive1[AuthedNamespaceScope] =
    extractCredentials flatMap { creds =>
      val maybeNamespace = creds match {
        case Some(OAuth2BearerToken(serializedToken)) =>
          NsFromToken.parseToken[T](serializedToken).flatMap{ token =>
            val authedNs = nsFromToken.toNamespaceScope(token)
            ns0 match {
              case Some(ns) if ns == authedNs.namespace => Right(authedNs)
              case Some(ns) if authedNs.hasScope(AuthedNamespaceScope.namespacePrefix + ns) =>
                Right(AuthedNamespaceScope(ns, authedNs.scope))
              case Some(ns) => Left("The oauth token does not accept the given namespace")
              case None => Right(authedNs)
            }
          }

        case _ => Left("No oauth token provided to extract namespace")
    }

    maybeNamespace match {
      case Right(t) => provide(t)
      case Left(msg) =>
        extractLog flatMap { l =>
          l.info(s"Could not extract namespace: $msg")
          reject(badNamespaceRejection(msg))
        }
    }
  }
}
