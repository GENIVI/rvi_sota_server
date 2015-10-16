/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import eu.timepit.refined._

object RefinedMarshallingSupport {

  implicit def refinedUnmarshaller[P](implicit p: Predicate[P, String]): FromStringUnmarshaller[Refined[String, P]] =
    Unmarshaller.strict[String, Refined[String, P]] { string =>
      refineV[P](string) match {
        case Left(e)  => throw new IllegalArgumentException(e)
        case Right(r) => r
      }
    }

  implicit def refinedFromRequestUnmarshaller[T, P]
  (implicit um: FromEntityUnmarshaller[T], p: Predicate[P, T])
  : FromRequestUnmarshaller[Refined[T, P]]
  = Unmarshaller { implicit ec => request =>
    um(request.entity).flatMap { (t: T) =>
      refineV[P](t) match {
        case Left(e)  => throw new DeserializationException(RefinementError(t, e))
        case Right(r) => FastFuture.successful(r)
      }
    }
  }

}
