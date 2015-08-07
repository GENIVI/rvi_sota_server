/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.refined

trait SprayJsonRefined {
  import eu.timepit.refined.{Predicate, _}
  import shapeless.tag.@@
  import spray.json._

  import scala.util.control.NoStackTrace
  import scala.util.{Left, Right}

  case class RefinmentError[T]( o: T, msg: String) extends NoStackTrace

  implicit def refinedTJsonFormat[T, P] (implicit delegate: JsonFormat[T], predicate: Predicate[P, T]) : JsonFormat[T @@ P] =
    new JsonFormat[T @@ P] {
      override def write( o: T @@ P ) : JsValue = delegate.write( o )

      override def read(value : JsValue) : T @@ P = {
        refineT[P]( delegate.read(value) ) match {
          case Right(x) => x
          case Left(e) => deserializationError(e, RefinmentError(value, e))
        }
      }
    }

  implicit def refinedJsonFormat[T, P](implicit delegate: JsonFormat[T], predicate: Predicate[P, T]) : JsonFormat[Refined[T, P]] =
    new JsonFormat[Refined[T, P]] {
      override def write(o: Refined[T, P]): JsValue = delegate.write(o.get)

      override def read(json: JsValue): Refined[T, P] = {
        val value = delegate.read(json)
        refineV[P]( value ) match {
          case Right(x) => x
          case Left(e) => deserializationError(e, RefinmentError(value, e))
        }
      }
    }
}

object SprayJsonRefined extends SprayJsonRefined
