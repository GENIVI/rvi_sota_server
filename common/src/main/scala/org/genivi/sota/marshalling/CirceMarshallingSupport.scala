/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpCharsets, MediaTypes}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import cats.data.Xor
import io.circe._

/**
  * Akka HTTP comes with built-in support for (un)marshalling using the
  * Spray JSON library, however since we are using Circe for JSON we
  * need to add this ourselves.
  *
  * @see {@link http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala.html}
  * @see {@link https://github.com/travisbrown/circe}
  */

trait CirceMarshallingSupport {

  // Unmarshalling

  implicit def circeUnmarshaller[T](implicit decoder: Decoder[T], mat: Materializer) : FromEntityUnmarshaller[T] =
    circeFromEntityJsonUnmarshaller
      .map( decoder.decodeJson )
      .flatMap( _ => _.fold( e => FastFuture.failed(new DeserializationException(e)), FastFuture.successful ) )

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

  // Marshalling

  implicit def circeJsonMarshaller(implicit printer: Printer) : ToEntityMarshaller[Json] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer.pretty)

  implicit def circeToEntityMarshaller[T](implicit encoder: Encoder[T], printer: Printer = Printer.noSpaces)
      : ToEntityMarshaller[T] =
    circeJsonMarshaller.compose(encoder.apply)


}

object CirceMarshallingSupport extends CirceMarshallingSupport with CirceInstances
