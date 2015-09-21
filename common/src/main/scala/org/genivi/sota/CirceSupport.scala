/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.{ContentTypes, HttpCharsets, MediaTypes, Uri}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import cats.data.Xor
import eu.timepit.refined.{Predicate, Refined, refineV}
import io.circe._
import java.util.UUID
import org.joda.time.{Interval, DateTime}
import org.joda.time.format.ISODateTimeFormat
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
        case Left(e)  =>
          println( e )
          throw new DeserializationException(RefinementError(t, e))
        case Right(r) => r
      })

  implicit def mapDecoder[K, V]
    (implicit keyDecoder: Decoder[K], valueDecoder: Decoder[V])
      : Decoder[Map[K, V]]
  = Decoder[Seq[(K, V)]].map(_.toMap)

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
          case Left(e)  => throw new DeserializationException(RefinementError(t, e))
          case Right(r) => FastFuture.successful(r)
        }
      }
  }

  // Marshalling

  implicit def circeJsonMarshaller(implicit printer: Printer) : ToEntityMarshaller[Json] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer.pretty)

  implicit def circeToEntityMarshaller[T](implicit encoder: Encoder[T], printer: Printer = Printer.noSpaces): ToEntityMarshaller[T] =
    circeJsonMarshaller.compose(encoder.apply)

  implicit def refinedEncoder[T, P](implicit encoder: Encoder[T]): Encoder[Refined[T, P]] =
    encoder.contramap(_.get)

  implicit val uriEncoder : Encoder[Uri] = Encoder.instance { uri =>
    Json.obj(("uri", Json.string(uri.toString)))
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
  implicit val uuidDecoder : Decoder[UUID] = Decoder[String].map(UUID.fromString(_))

  implicit val dateTimeEncoder : Encoder[DateTime] = Encoder.instance[DateTime]( x =>  Json.string( ISODateTimeFormat.dateTime().print(x)) )
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
  implicit val intervalDecoder : Decoder[Interval] = Decoder[String].map(Interval.parse(_))

  implicit def mapEncoder[K, V]
    (implicit keyEncoder: Encoder[K], valueEncoder: Encoder[V])
      : Encoder[Map[K, V]]
  = Encoder[Seq[(K, V)]].contramap((m: Map[K, V]) => m.toSeq)

}
