/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.http.scaladsl.server.{Directives, Directive1, ValidationRejection}


object Validation extends Directives {

  import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
  import eu.timepit.refined.{Predicate, refineT}
  import shapeless.tag.@@

  trait Valid

  def validated[A](implicit um: FromRequestUnmarshaller[A], p: Predicate[Valid, A]): Directive1[A @@ Valid] = {
    entity[A](um).flatMap { a: A =>
      refineT[Valid](a) match {
        case Left(e)  => reject(ValidationRejection(e))
        case Right(b) => provide(b)
      }
    }
  }

  import akka.http.scaladsl.model.Uri._
  import org.genivi.sota.resolver.types.Vin
  import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
  import akka.http.scaladsl.server.PathMatchers

  def validatedPut[A](parser: String => Either[String, A])(implicit p: Predicate[Valid, A]): Directive1[A @@ Valid] =
    extractRequestContext.flatMap[Tuple1[A @@ Valid]] { ctx =>
      import ctx.executionContext
      val pathMatcher = PathMatchers.Slash ~ Segment ~ PathEnd
      pathMatcher(ctx.unmatchedPath) match {
        case Matched(Path.Empty, Tuple1(str)) => parser(str) match {
          case Left(e)  => reject(ValidationRejection(e))
          case Right(a) => refineT[Valid](a) match {
            case Left(e)  => reject(ValidationRejection(e))
            case Right(b) => provide(b)
          }
        }
        case _                                => reject
      }
    }
}
