/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.Uri._
import akka.http.scaladsl.server.PathMatcher.Matched
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import eu.timepit.refined.boolean.And
import eu.timepit.refined.{Refined, Predicate, refineT, refineV}
import scala.reflect.ClassTag
import shapeless.tag.@@


object Validation extends Directives {

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

  trait Valid

  def validatedPost2[A, B](implicit um: FromRequestUnmarshaller[A], p: Predicate[B, A]): Directive1[A @@ B] = {
    entity[A](um).flatMap { a: A =>
      refineT[B](a) match {
        case Left(e)  => reject(ValidationRejection(e))
        case Right(b) => provide(b)
      }
    }
  }

  def validatedPost[A](implicit um: FromRequestUnmarshaller[A], p: Predicate[Valid, A]): Directive1[A @@ Valid] =
    validatedPost2[A, Valid]

  def taggedSegment[P, A](parser: String => Either[String, A])(implicit p: Predicate[P, A]): Directive1[A @@ P] =
    extractRequestContext.flatMap[Tuple1[A @@ P]] { ctx =>
      val pathMatcher = PathMatchers.Slash ~ Segment ~ PathEnd
      pathMatcher(ctx.unmatchedPath) match {
        case Matched(Path.Empty, Tuple1(str)) => parser(str) match {
          case Left(e)  => reject(ValidationRejection(e))
          case Right(a) => refineT[P](a) match {
            case Left(e)  => reject(ValidationRejection(e))
            case Right(b) => provide(b)
          }
        }
        case _                                => reject
      }
    }

  def refined[T, P](wrapped: PathMatcher1[T])
                   (implicit p: Predicate[P, T], ev: ClassTag[T]): Directive1[T Refined P] = {
    extractRequestContext.flatMap[Tuple1[T Refined P]] { ctx =>
      val pathMatcher = PathMatchers.Slash ~ wrapped ~ PathEnd
      pathMatcher(ctx.unmatchedPath) match {
        case Matched(Path.Empty, Tuple1(x: T)) =>
          refineV[P](x).fold( e => reject(ValidationRejection(e)), a => provide(a))
        case _                                => reject
      }
    }

  }

  def vpost[A](k: A @@ Valid => ToResponseMarshallable)
    (implicit um: FromRequestUnmarshaller[A], p: Predicate[Valid, A]): Route =
      (post & validatedPost[A]) { v => complete(k(v)) }

  def vput[A](parser: String => Either[String, A])(k: A @@ Valid => ToResponseMarshallable)
    (implicit um: FromRequestUnmarshaller[A], p: Predicate[Valid, A]): Route =
      (put & taggedSegment[Valid, A](parser)) { v => complete(k(v)) }
}
