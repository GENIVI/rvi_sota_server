/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server._
import eu.timepit.refined.boolean.And
import eu.timepit.refined.{Refined, Predicate, refineV}
import scala.reflect.ClassTag


final class RefinedMatcher[P] {
  import Directives._
  def apply[T]( pm: PathMatcher1[T] )(implicit p: Predicate[P, T], ev: ClassTag[T]) : Directive1[T Refined P] =
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
  import Directives._

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

  def refined[P]: RefinedMatcher[P] = new RefinedMatcher[P]

}
