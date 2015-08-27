/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.refined

import eu.timepit.refined.{Predicate, Refined, refineV}
import spray.json._
import scala.util.control.NoStackTrace


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
}

object SprayJsonRefined extends SprayJsonRefined
