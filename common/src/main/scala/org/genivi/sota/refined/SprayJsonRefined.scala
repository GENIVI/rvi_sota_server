/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.refined

import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller, FromEntityUnmarshaller}
import eu.timepit.refined.{Predicate, Refined, refineV}
import akka.http.scaladsl.util.FastFuture.successful
import spray.json._
import scala.util.control.NoStackTrace
import akka.http.scaladsl.unmarshalling._


trait SprayJsonRefined {

  case class RefinementError[T]( o: T, msg: String) extends NoStackTrace

  implicit def refinedJsonFormat[T, P](implicit delegate: JsonFormat[T], p: Predicate[P, T]): JsonFormat[Refined[T, P]] =
    new JsonFormat[Refined[T, P]] {
      override def write(o: Refined[T, P]): JsValue = delegate.write(o.get)

      override def read(json: JsValue): Refined[T, P] = {
        val value = delegate.read(json)
        refineV[P]( value ) match {
          case Right(x) => x
          case Left(e)  => deserializationError(e, RefinementError(value, e))
        }
      }
    }

  implicit def refinedUnmarshaller[P]
    (implicit p: Predicate[P, String])
      : FromStringUnmarshaller[Refined[String, P]]
  = Unmarshaller.strict[String, Refined[String, P]] { string =>
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
          case Left(e)  => deserializationError(e, RefinementError(t, e))
          case Right(r) => successful(r)
        }
      }
  }

}

object SprayJsonRefined extends SprayJsonRefined
