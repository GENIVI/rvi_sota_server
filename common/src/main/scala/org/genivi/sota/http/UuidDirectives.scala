package org.genivi.sota.http

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Directives}
import Directives._
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.rest.Validation._

import scala.concurrent.Future

object UuidDirectives {
  val extractUuid: Directive1[Uuid] = refined[Uuid.Valid](Slash ~ Segment).map(Uuid(_))
  val extractRefinedUuid = refined[Uuid.Valid](Slash ~ Segment)

  def allowExtractor[T](namespaceExtractor: Directive1[AuthedNamespaceScope],
                        extractor: Directive1[T],
                        allowFn: (T => Future[Namespace])): Directive1[T] = {
    (extractor & namespaceExtractor).tflatMap { case (value, ans) =>
      onSuccess(allowFn(value)).flatMap {
        case namespace if namespace == ans.namespace =>
          provide(value)
        case _ =>
          reject(AuthorizationFailedRejection)
      }
    }
  }
}
