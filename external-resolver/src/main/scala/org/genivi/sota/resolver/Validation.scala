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
        case Left(e)  => reject(ValidationRejection(e, None))
        case Right(b) => provide(b)
      }
    }
  }
}
