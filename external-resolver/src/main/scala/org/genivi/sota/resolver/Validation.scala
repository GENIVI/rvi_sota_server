/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.http.scaladsl.server.{Directives, Directive1, ValidationRejection}


object Validation extends Directives {

  import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
  import eu.timepit.refined.{Predicate, refineT}
  import eu.timepit.refined.boolean.And
  import shapeless.tag.@@

  trait Valid

  implicit def andPredicate[A, B, T](implicit pa: Predicate[A, T], pb: Predicate[B, T]): Predicate[A And B, T] =
    new Predicate[A And B, T] {
      def isValid(t: T): Boolean = pa.isValid(t) && pb.isValid(t)
      def show(t: T): String = s"(${pa.show(t)} && ${pb.show(t)})"

      override def validate(t: T): Option[String] =
        (pa.validate(t), pb.validate(t)) match {
          case (Some(sl), Some(sr)) =>
            Some(s"$sl, $sr")
          case (Some(sl), None) =>
            Some( pa.show(t) )
          case (None, Some(sr)) =>
            Some( pb.show(t) )
          case _ => None
        }

      override val isConstant: Boolean = pa.isConstant && pb.isConstant
    }

  def validated[A, B](implicit um: FromRequestUnmarshaller[A], p: Predicate[B, A]): Directive1[A @@ B] = {
    entity[A](um).flatMap { a: A =>
      refineT[B](a) match {
        case Left(e)  => reject(ValidationRejection(e, None))
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
