/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import java.util.UUID

import akka.http.scaladsl.model.Uri
import cats.data.Xor
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Refined, Validate}
import io.circe.{DecodingFailure, Json, Encoder, Decoder}
import org.joda.time.{Interval, DateTime}
import org.joda.time.format.ISODateTimeFormat

/**
  * Some datatypes we use don't have predefined JSON encoders and
  * decoders in Circe, so we add them here.
  *
  * @see {@link https://github.com/travisbrown/circe}
  */

trait CirceInstances {

  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], p: Validate.Plain[T, P]): Decoder[Refined[T, P]] =
    decoder.map(t =>
      refineV[P](t) match {
        case Left(e)  =>
          throw new DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })

  implicit def mapDecoder[K, V](implicit keyDecoder: Decoder[K], valueDecoder: Decoder[V]): Decoder[Map[K, V]] =
    Decoder[Seq[(K, V)]].map(_.toMap)

  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.get)

  implicit val uriEncoder : Encoder[Uri] = Encoder.instance { uri =>
    Json.obj(("uri", Json.string(uri.toString())))
  }

  implicit val uriDecoder : Decoder[Uri] = Decoder.instance { c =>
    c.focus.asObject match {
      case None      => Xor.left(DecodingFailure("Uri", c.history))
      case Some(obj) => obj.toMap.get("uri").flatMap(_.asString) match {
        case None      => Xor.left(DecodingFailure("Uri", c.history))
        case Some(uri) => Xor.right(Uri(uri))
      }
    }
  }

  implicit val uuidEncoder : Encoder[UUID] = Encoder[String].contramap(_.toString)
  implicit val uuidDecoder : Decoder[UUID] = Decoder[String].map(UUID.fromString)

  implicit val dateTimeEncoder : Encoder[DateTime] =
    Encoder.instance[DateTime]( x =>  Json.string( ISODateTimeFormat.dateTime().print(x)) )

  implicit val dateTimeDecoder : Decoder[DateTime] = Decoder.instance { c =>
    c.focus.asString match {
      case None       => Xor.left(DecodingFailure("DataTime", c.history))
      case Some(date) => try { Xor.right(ISODateTimeFormat.dateTimeNoMillis().parseDateTime(date)) }
      catch {
        case _: IllegalArgumentException => Xor.left(DecodingFailure("DateTime", c.history))
      }
    }
  }

  implicit val intervalEncoder : Encoder[Interval] = Encoder[String].contramap(_.toString)
  implicit val intervalDecoder : Decoder[Interval] = Decoder[String].map(Interval.parse)

  implicit def mapEncoder[K, V](implicit keyEncoder: Encoder[K], valueEncoder: Encoder[V]): Encoder[Map[K, V]] =
    Encoder[Seq[(K, V)]].contramap((m: Map[K, V]) => m.toSeq)


}

object CirceInstances extends CirceInstances
