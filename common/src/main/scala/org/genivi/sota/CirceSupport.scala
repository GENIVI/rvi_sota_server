/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller, ToResponseMarshaller }
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.{ ContentTypes, HttpCharsets, MediaTypes }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, FromRequestUnmarshaller, FromResponseUnmarshaller, Unmarshaller }
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import cats.data.Xor
import eu.timepit.refined.{Predicate, Refined, refineV}
import io.circe._
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace


object CirceSupport {
  case class DeserializationException(cause: Throwable) extends Throwable(cause)

  // Unmarshalling

  implicit def circeUnmarshaller[T](implicit decoder: Decoder[T], mat: Materializer) : FromEntityUnmarshaller[T] =
    circeFromEntityJsonUnmarshaller.map( decoder.decodeJson ).flatMap(_ => _.fold( e => FastFuture.failed(new DeserializationException(e)), FastFuture.successful ))

  implicit def circeFromEntityJsonUnmarshaller(implicit mat: Materializer) : FromEntityUnmarshaller[Json] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        jawn.parse(input) match {
          case Xor.Right(json) => json
          case Xor.Left(e)     => throw new DeserializationException(e)
        }
      }

  case class RefinementError[T]( o: T, msg: String) extends NoStackTrace


  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], predicate: Predicate[P, T]): Decoder[Refined[T, P]] =
    decoder.map(t =>
      refineV[P](t) match {
        case Left(e)  => throw new DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })

  // Marshalling

  implicit def circeJsonMarshaller(implicit printer: Printer) : ToEntityMarshaller[Json] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer.pretty)

  implicit def circeToEntityMarshaller[T](implicit encoder: Encoder[T], printer: Printer = Printer.noSpaces): ToEntityMarshaller[T] =
    circeJsonMarshaller.compose(encoder.apply)

  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.get)

}
