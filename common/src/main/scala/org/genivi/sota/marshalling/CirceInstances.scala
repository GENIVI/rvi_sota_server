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
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.joda.time.{DateTime, Interval}
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

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
    Json.obj(("uri", Json.fromString(uri.toString())))
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
    Encoder.instance[DateTime]( x =>  Json.fromString( ISODateTimeFormat.dateTime().print(x)) )

  implicit val dateTimeDecoder : Decoder[DateTime] = Decoder.instance { c =>
    c.focus.asString match {
      case None       => Xor.left(DecodingFailure("DataTime", c.history))
      case Some(date) =>
        val parsers = List(ISODateTimeFormat.dateTimeNoMillis(), ISODateTimeFormat.dateTime())
        tryParsers(date, parsers, DecodingFailure("DateTime", c.history))
    }
  }

  @tailrec
  private def tryParsers(string: String, parsers: List[DateTimeFormatter],
                         error: DecodingFailure): Xor[DecodingFailure, DateTime] = {
    parsers match {
      case parser :: otherParsers =>
        try { Xor.right(parser.parseDateTime(string)) }
        catch {
          case t: IllegalArgumentException =>
            tryParsers(string, otherParsers, error)
        }
      case Nil => Xor.left(error)
    }
  }

  implicit val intervalEncoder : Encoder[Interval] = Encoder[String].contramap(_.toString)
  implicit val intervalDecoder : Decoder[Interval] = Decoder[String].map(Interval.parse)

  implicit def mapEncoder[K, V](implicit keyEncoder: Encoder[K], valueEncoder: Encoder[V]): Encoder[Map[K, V]] =
    Encoder[Seq[(K, V)]].contramap((m: Map[K, V]) => m.toSeq)


}

object CirceInstances extends CirceInstances
