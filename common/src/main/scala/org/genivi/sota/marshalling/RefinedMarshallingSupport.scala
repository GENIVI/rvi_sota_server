/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Validate, Refined}

/**
  * Add Akka HTTP request unmarshalling support for refined types.
  *
  * @see {@link http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala.html}
  * @see {@link https://github.com/fthomas/refined}
  */

object RefinedMarshallingSupport {

  implicit def refinedUnmarshaller[P]
    (implicit p: Validate.Plain[String, P]): FromStringUnmarshaller[Refined[String, P]] =
    Unmarshaller.strict[String, Refined[String, P]] { string =>
      refineV[P](string) match {
        case Left(e)  => throw new IllegalArgumentException(e)
        case Right(r) => r
      }
    }

  implicit def refinedFromRequestUnmarshaller[T, P]
  (implicit um: FromEntityUnmarshaller[T], p: Validate.Plain[T, P])
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
