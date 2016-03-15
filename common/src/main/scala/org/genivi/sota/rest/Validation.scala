/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server._
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Refined, Validate}
import scala.reflect.ClassTag

/**
  * Pathmatchers in Akka HTTP enable the programmer to extract values
  * from the URL. Here we extend the pathmatching facility to do
  * validation using the predicate of our refined types. If validation
  * fails, reject the request -- see Handlers.scala for how this is
  * handled.
  *
  * @see {@link http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala.html}
  * @see {@link https://github.com/fthomas/refined}
  */

final class RefinedMatcher[P] {
  import Directives._
  def apply[T]( pm: PathMatcher1[T] )(implicit p: Validate.Plain[T, P], ev: ClassTag[T]) : Directive1[T Refined P] =
    extractRequestContext.flatMap[Tuple1[T Refined P]] { ctx =>
      pm(ctx.unmatchedPath) match {
        case Matched(rest, Tuple1(t: T)) => refineV[P](t) match {
          case Left(err)      => reject(ValidationRejection(err))
          case Right(refined) => provide(refined) & mapRequestContext(_ withUnmatchedPath rest)
        }
        case Unmatched                   => reject
      }
    }
}

object Validation {

  def refined[P]: RefinedMatcher[P] = new RefinedMatcher[P]

}
