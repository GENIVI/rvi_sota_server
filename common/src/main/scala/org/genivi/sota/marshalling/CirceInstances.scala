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
import io.circe._
import org.genivi.sota.data.{Device, Interval, PackageId}
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import cats.Show
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

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
          throw DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })

  implicit def refinedMapDecoder[K <: Refined[_, _], V]
  (implicit keyDecoder: Decoder[K], valueDecoder: Decoder[V]): Decoder[Map[K, V]] =
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

  implicit val dateTimeEncoder : Encoder[Instant] =
    Encoder.instance[Instant]( x =>  Json.fromString( x.toString) )

  implicit val dateTimeDecoder : Decoder[Instant] = Decoder.instance { c =>
    c.focus.asString match {
      case None       => Xor.left(DecodingFailure("DataTime", c.history))
      case Some(date) =>
        tryParser(date, DecodingFailure("DateTime", c.history))
    }
  }

  /**
    * It can parse:
    * 2016-06-10T09:47:33.465789+0000
    * 2016-06-10T09:47:33.465789+01:01
    *
    * But not:
    * 2016-06-10T09:47:33.465789+00
    * 2011-12-03T10:15:30+01:00[Europe/Paris]
    */
  private def tryParser(input: String, error: DecodingFailure): Xor[DecodingFailure, Instant] = {
    try {
      val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
      val nst = Instant.from(fmt.parse(input))
      Xor.right(nst)
    }
    catch {
      case t: DateTimeParseException =>
        Xor.left(error)
    }
  }

  implicit val intervalEncoder : Encoder[Interval] = Encoder[String].contramap(_.toString)
  implicit val intervalDecoder : Decoder[Interval] = Decoder[String].map(Interval.parse)

  implicit def mapRefinedEncoder[K <: Refined[_, _], V]
  (implicit keyEncoder: Encoder[K], valueEncoder: Encoder[V]): Encoder[Map[K, V]] =
    Encoder[Seq[(K, V)]].contramap((m: Map[K, V]) => m.toSeq)

  implicit val packageIdEncoder : Encoder[PackageId] = deriveEncoder[PackageId]
  implicit val packageIdDecoder : Decoder[PackageId] = deriveDecoder[PackageId]

  // Circe encoding

  import io.circe._

  // TODO generalize to refined and showable value class decoder/encoder
  implicit val idEncoder: Encoder[Device.Id] = Encoder[String].contramap(implicitly[Show[Device.Id]].show(_))
  implicit val idDecoder: Decoder[Device.Id] = refinedDecoder[String, Device.ValidId].map(Device.Id(_))

  implicit val deviceNameEncoder: Encoder[Device.DeviceName] =
    Encoder[String].contramap(implicitly[Show[Device.DeviceName]].show(_))
  implicit val deviceNameDecoder: Decoder[Device.DeviceName] = Decoder[String].map(Device.DeviceName(_))

  implicit val deviceIdEncoder: Encoder[Device.DeviceId] =
    Encoder[String].contramap(implicitly[Show[Device.DeviceId]].show(_))
  implicit val deviceIdDecoder: Decoder[Device.DeviceId] = Decoder[String].map(Device.DeviceId(_))
}

object CirceInstances extends CirceInstances
