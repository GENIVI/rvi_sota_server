/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import akka.http.scaladsl.model.Uri
import cats.Show
import cats.syntax.show.toShowOps
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import io.circe._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.util.UUID
import org.genivi.sota.data.{Device, Interval, PackageId, Uuid}


/**
  * Some datatypes we use don't have predefined JSON encoders and
  * decoders in Circe, so we add them here.
  *
  * @see {@link https://github.com/travisbrown/circe}
  */

trait CirceInstances {

  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.get)

  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], p: Validate.Plain[T, P]): Decoder[Refined[T, P]] =
    decoder.map(t =>
      refineV[P](t) match {
        case Left(e)  =>
          throw DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })

  implicit def refinedMapEncoder[K <: Refined[_, _], V]
  (implicit keyEncoder: Encoder[K], valueEncoder: Encoder[V]): Encoder[Map[K, V]] =
    Encoder[Seq[(K, V)]].contramap((m: Map[K, V]) => m.toSeq)

  implicit def refinedMapDecoder[K <: Refined[_, _], V]
  (implicit keyDecoder: Decoder[K], valueDecoder: Decoder[V]): Decoder[Map[K, V]] =
    Decoder[Seq[(K, V)]].map(_.toMap)

  implicit val uriEncoder : Encoder[Uri] = Encoder.instance { uri =>
    Json.obj(("uri", Json.fromString(uri.toString())))
  }

  implicit val uriDecoder : Decoder[Uri] = Decoder.instance { c =>
    c.focus match {
      case None      => Left(DecodingFailure("Uri", c.history))
      case Some(obj) => obj.hcursor.downField("uri").focus.flatMap(_.asString) match {
        case None      => Left(DecodingFailure("Uri", c.history))
        case Some(uri) => Right(Uri(uri))
      }
    }
  }

  implicit val javaUuidEncoder : Encoder[UUID] = Encoder[String].contramap(_.toString)
  implicit val javaUuidDecoder : Decoder[UUID] = Decoder[String].map(UUID.fromString)

  // PRO-1297: once done we only need one uuid {en|de}coder
  implicit val customUuidEncoder : Encoder[Uuid] = Encoder[String].contramap(_.show)
  implicit val customUuidDecoder : Decoder[Uuid] =
    refinedDecoder[String, Uuid.Valid].map(Uuid(_))

  // TODO AnyVal key decoder, see below
  implicit val customUuidKeyDecoder: KeyDecoder[Uuid] =
  KeyDecoder.instance[Uuid] { k =>
    refineV[Uuid.Valid](k).right.toOption.map(Uuid(_))
  }

  implicit val dateTimeEncoder : Encoder[Instant] =
    Encoder.instance[Instant]( x =>  Json.fromString( x.toString) )

  implicit val dateTimeDecoder : Decoder[Instant] = Decoder.instance { c =>
    c.focus.flatMap(_.asString) match {
      case None       => Left(DecodingFailure("DataTime", c.history))
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
  private def tryParser(input: String, error: DecodingFailure): Either[DecodingFailure, Instant] = {
    try {
      val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
      val nst = Instant.from(fmt.parse(input))
      Right(nst)
    }
    catch {
      case t: DateTimeParseException =>
        Left(error)
    }
  }

  implicit val intervalEncoder : Encoder[Interval] = Encoder[String].contramap(_.toString)
  implicit val intervalDecoder : Decoder[Interval] = Decoder[String].map(Interval.parse)

  implicit val packageIdEncoder : Encoder[PackageId] = deriveEncoder[PackageId]
  implicit val packageIdDecoder : Decoder[PackageId] = deriveDecoder[PackageId]

  // Circe encoding

  import io.circe._

  // TODO generalize to refined and showable value class decoder/encoder
  implicit val deviceNameEncoder: Encoder[Device.DeviceName] = Encoder[String].contramap(_.show)
  implicit val deviceNameDecoder: Decoder[Device.DeviceName] = Decoder[String].map(Device.DeviceName)

  implicit val deviceIdEncoder: Encoder[Device.DeviceId] = Encoder[String].contramap(_.show)
  implicit val deviceIdDecoder: Decoder[Device.DeviceId] = Decoder[String].map(Device.DeviceId)

  implicit def showableKeyEncoder[K](implicit show: Show[K]): KeyEncoder[K] =
    KeyEncoder.instance[K](_.show)

  import shapeless._
  import shapeless.ops.hlist.IsHCons
  import shapeless.{Generic, HList, HNil}

  implicit def anyValUnwrappedEncoder[A <: AnyVal, B <: HList, C](implicit gen: Generic.Aux[A, B],
  hCons: IsHCons.Aux[B, C, HNil],
  wrappedEncoder: Encoder[C] ): Encoder[A] =
  wrappedEncoder.contramap[A](a => gen.to(a).head)

  implicit def anyValWrapDecoder[A <: AnyVal, B <: HList, C](implicit gen: Generic.Aux[A, B],
                                                             ev: (C :: HNil) =:= B,
                                                             wrappedDecoder: Decoder[C] ): Decoder[A] = {
    wrappedDecoder.map{ x =>
      gen.from(ev(x :: HNil))
    }
  }
}

object CirceInstances extends CirceInstances
