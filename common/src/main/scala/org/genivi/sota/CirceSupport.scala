/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota

import akka.http.scaladsl.util.FastFuture

object CirceSupport {
  import io.circe._
  import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
  import akka.http.scaladsl.model.{ ContentTypes, HttpCharsets, MediaTypes }
  import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
  import akka.stream.Materializer
  import cats.data.Xor._
  import eu.timepit.refined.Predicate
  import eu.timepit.refined.Refined

  implicit def circeUnmarshaller[T](implicit decoder: Decoder[T], mat: Materializer) : FromEntityUnmarshaller[T] =
    circeJsonUnmarshaller.map( decoder.decodeJson ).flatMap(ec => x => x.fold( FastFuture.failed, FastFuture.successful ))

  implicit def circeJsonUnmarshaller(implicit mat: Materializer) : FromEntityUnmarshaller[Json] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        jawn.parse(input) match {
          case Right(json) => json
          case Left(e) => throw e
        }
      }

  implicit def circeMarshaller[T](implicit encoder: Encoder[T], printer: Printer = Printer.noSpaces) : ToEntityMarshaller[T] =
    circeJsonMarshaller.compose(encoder.apply)

  implicit def circeJsonMarshaller(implicit printer: Printer) : ToEntityMarshaller[Json] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer.pretty)

  implicit def refinedDecoder[T, P](implicit decoder: Decoder[T], predicate: Predicate[P, T]) : Decoder[Refined[T, P]] = decoder.map( Refined.apply )

}
